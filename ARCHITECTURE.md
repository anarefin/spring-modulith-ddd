# Architecture Documentation

## Modular Monolithic Architecture

This document provides detailed architectural insights into the modular monolithic design.

## Module Structure

### 1. Product Module

**Responsibility**: Product catalog management

**Database Schema**: `product_schema`

**Domain Model**:
```java
Product {
  - id: Long
  - name: String
  - description: String
  - price: BigDecimal
  - stock: Integer
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
}
```

**Public Service Interface**:
```java
interface ProductService {
  Product createProduct(Product product)
  Optional<Product> getProductById(Long id)
  List<Product> getAllProducts()
  void reduceStock(Long productId, Integer quantity)  // Called by Order module
  boolean hasAvailableStock(Long productId, Integer quantity)  // Called by Order module
}
```

**REST Endpoints**:
- POST `/api/products` - Create product
- GET `/api/products` - Get all products
- GET `/api/products/{id}` - Get product by ID
- GET `/api/products/available` - Get products with stock > 0
- PUT `/api/products/{id}` - Update product
- DELETE `/api/products/{id}` - Delete product

**Dependencies**: None (no inter-module dependencies)

---

### 2. Order Module

**Responsibility**: Order creation and management

**Database Schema**: `order_schema`

**Domain Model**:
```java
Order {
  - id: Long
  - productId: Long  // Reference only, no FK constraint
  - productName: String  // Denormalized data snapshot
  - quantity: Integer
  - totalAmount: BigDecimal
  - status: OrderStatus (PENDING, PAID, FAILED, CANCELLED)
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
}
```

**Public Service Interface**:
```java
interface OrderService {
  Order createOrder(Long productId, Integer quantity)
  Optional<Order> getOrderById(Long id)
  List<Order> getAllOrders()
  void updateOrderStatus(Long orderId, OrderStatus status)  // Called by Payment module
}
```

**REST Endpoints**:
- POST `/api/orders` - Create order
- GET `/api/orders` - Get all orders
- GET `/api/orders/{id}` - Get order by ID
- GET `/api/orders/status/{status}` - Get orders by status
- PUT `/api/orders/{id}/cancel` - Cancel order

**Dependencies**: 
- **ProductService** (injected) - Used to validate products and reduce stock

**Inter-Module Calls**:
```java
// In OrderServiceImpl.createOrder()
Product product = productService.getProductById(productId);  // Validate product exists
boolean hasStock = productService.hasAvailableStock(productId, quantity);  // Check stock
productService.reduceStock(productId, quantity);  // Reduce stock atomically
```

---

### 3. Payment Module

**Responsibility**: Payment processing

**Database Schema**: `payment_schema`

**Domain Model**:
```java
Payment {
  - id: Long
  - orderId: Long  // Reference only, no FK constraint
  - amount: BigDecimal
  - status: PaymentStatus (PENDING, SUCCESS, FAILED, REFUNDED)
  - paymentMethod: String
  - transactionId: String
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
}
```

**Public Service Interface**:
```java
interface PaymentService {
  Payment processPayment(Long orderId, String paymentMethod)
  Optional<Payment> getPaymentById(Long id)
  Optional<Payment> getPaymentByOrderId(Long orderId)
  List<Payment> getAllPayments()
}
```

**REST Endpoints**:
- POST `/api/payments` - Process payment
- GET `/api/payments` - Get all payments
- GET `/api/payments/{id}` - Get payment by ID
- GET `/api/payments/order/{orderId}` - Get payment by order ID
- GET `/api/payments/status/{status}` - Get payments by status

**Dependencies**: 
- **OrderService** (injected) - Used to validate orders and update order status

**Inter-Module Calls**:
```java
// In PaymentServiceImpl.processPayment()
Order order = orderService.getOrderById(orderId);  // Validate order exists
// After successful payment:
orderService.updateOrderStatus(orderId, OrderStatus.PAID);  // Update order status
```

---

## Module Dependency Graph

```
┌─────────────────┐
│  Product Module │
│                 │
│  No Dependencies│
└────────▲────────┘
         │
         │ ProductService
         │
┌────────┴────────┐
│  Order Module   │
│                 │
│  Depends on:    │
│  - Product      │
└────────▲────────┘
         │
         │ OrderService
         │
┌────────┴────────┐
│  Payment Module │
│                 │
│  Depends on:    │
│  - Order        │
└─────────────────┘
```

## Database Schema Isolation

### Schema Design

```sql
-- Each module has its own schema
CREATE SCHEMA product_schema;
CREATE SCHEMA order_schema;
CREATE SCHEMA payment_schema;

-- Product Module owns product_schema
product_schema.products

-- Order Module owns order_schema
order_schema.orders

-- Payment Module owns payment_schema
payment_schema.payments
```

### No Cross-Schema Foreign Keys

Unlike a traditional monolithic database with foreign key constraints across tables, this architecture intentionally **avoids foreign keys between schemas**:

```sql
-- ❌ Traditional Monolith (tight coupling)
CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT REFERENCES products(id),  -- FK constraint!
  ...
);

-- ✅ Modular Monolith (loose coupling)
CREATE TABLE order_schema.orders (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT,  -- Just a reference, no FK!
  product_name VARCHAR(255),  -- Snapshot/denormalized data
  ...
);
```

**Benefits**:
1. Modules can be extracted to separate databases easily
2. No database-level coupling between modules
3. Each module owns its data completely
4. Referential integrity enforced at service layer

## Transaction Boundaries

### Module-Level Transactions

Each module manages its own transactions:

```java
@Service
@Transactional  // Transaction scope: Order module only
public class OrderServiceImpl implements OrderService {
    
    public Order createOrder(Long productId, Integer quantity) {
        // This transaction only covers order_schema
        Order order = new Order();
        // ... set properties
        orderRepository.save(order);
        
        // This is a SEPARATE transaction in product_schema
        productService.reduceStock(productId, quantity);
        
        return order;
    }
}
```

### No Distributed Transactions

This POC demonstrates the **saga pattern** (orchestration-based):

```
Order Creation Flow (Saga):
1. Create Order (in order_schema) ✓
2. Reduce Stock (in product_schema) ✓
   - If fails → compensating transaction needed (cancel order)
   
Payment Flow (Saga):
1. Process Payment (in payment_schema) ✓
2. Update Order Status (in order_schema) ✓
   - If fails → mark payment as refunded
```

For production, consider:
- Saga orchestration frameworks (e.g., Camunda)
- Event sourcing for compensation
- Idempotency keys for retries

## Inter-Module Communication

### Direct Method Calls (Current Implementation)

```java
// Order Module depends on Product Module
@Service
public class OrderServiceImpl {
    private final ProductService productService;  // Injected
    
    public Order createOrder(...) {
        productService.hasAvailableStock(...);  // Direct method call
    }
}
```

**Advantages**:
- Simple and fast
- No serialization overhead
- Compile-time type safety
- Easy debugging

**Disadvantages**:
- Tight runtime coupling
- All modules must run in same JVM

### Future: Migration to Events/Messages

For microservices extraction, replace direct calls with events:

```java
// Instead of: productService.reduceStock(...)
// Publish event: eventPublisher.publish(new StockReductionRequested(productId, quantity))
```

## Scalability & Performance

### Current State (Modular Monolith)

- **Single JVM**: All modules run in one process
- **Vertical Scaling**: Add more CPU/memory to single container
- **Connection Pool**: Single database connection pool shared by all modules
- **Thread Pool**: Shared thread pool for all modules

### Migration Path (Microservices)

Each module can be extracted independently:

1. **Product Microservice**:
   - Move `product_schema` to separate PostgreSQL database
   - Deploy product module as standalone service
   - Replace direct calls with REST/gRPC

2. **Order Microservice**:
   - Move `order_schema` to separate database
   - Update inter-module calls to HTTP clients

3. **Payment Microservice**:
   - Move `payment_schema` to separate database
   - Implement retry and circuit breaker patterns

## Testing Strategy

### Unit Tests

Test each module independently:

```java
@SpringBootTest
class ProductServiceTest {
    @Autowired ProductService productService;
    
    @Test
    void shouldCreateProduct() { ... }
}
```

### Integration Tests

Test inter-module communication:

```java
@SpringBootTest
class OrderIntegrationTest {
    @Autowired OrderService orderService;
    @Autowired ProductService productService;
    
    @Test
    void shouldCreateOrderAndReduceStock() {
        // Verify inter-module call works
    }
}
```

### End-to-End Tests

Test complete flows across all modules:

```bash
# Use test-api.sh script
./test-api.sh
```

## Monitoring & Observability

### Logging

Each module logs with module prefix:

```
2025-10-16 10:23:45 INFO  [Product] Creating product: Dell XPS 15
2025-10-16 10:23:46 INFO  [Order] Creating order for product 1
2025-10-16 10:23:46 INFO  [Order] Reducing stock for product 1 by 2
2025-10-16 10:23:47 INFO  [Payment] Processing payment for order 1
2025-10-16 10:23:47 INFO  [Payment] Updating order status to PAID
```

### Metrics (Future Enhancement)

Add Spring Boot Actuator:
- `/actuator/health` - Health checks per module
- `/actuator/metrics` - Performance metrics
- `/actuator/prometheus` - Prometheus integration

### Distributed Tracing (Future Enhancement)

Add Spring Cloud Sleuth/OpenTelemetry:
- Trace requests across module boundaries
- Track inter-module call chains
- Identify performance bottlenecks

## Security Considerations

### Current State

- All modules share same security context
- Single authentication/authorization

### Future Enhancements

- Module-level authentication
- API Gateway pattern
- OAuth2/JWT tokens
- Rate limiting per module

## Deployment

### Current: Single Container

```yaml
# docker-compose.yml
services:
  app:
    image: modular-monolith-app
    # All modules run in single container
```

### Future: Kubernetes Deployment

Each module can be deployed as separate pods:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
spec:
  replicas: 3  # Scale product module independently
  ...
```

## Best Practices Demonstrated

1. ✅ **Clear Module Boundaries**: Each module in separate package
2. ✅ **Schema Isolation**: Dedicated database schema per module
3. ✅ **Service Interfaces**: Clean API contracts between modules
4. ✅ **No Cross-Schema Queries**: Enforced at application layer
5. ✅ **Data Denormalization**: Store snapshots to avoid dependencies
6. ✅ **Separate Transactions**: Each module manages own transactions
7. ✅ **Migration Ready**: Easy path to microservices

## Common Pitfalls to Avoid

❌ **Don't**: Query directly across schemas
```sql
-- Bad!
SELECT * FROM product_schema.products p
JOIN order_schema.orders o ON p.id = o.product_id;
```

✅ **Do**: Use service layer
```java
// Good!
Product product = productService.getProductById(order.getProductId());
```

❌ **Don't**: Share domain models between modules
```java
// Bad!
import com.demo.modular.product.domain.Product;
// in Order module - creates coupling
```

✅ **Do**: Use DTOs or store snapshots
```java
// Good!
order.setProductName(product.getName());  // Store snapshot
```

❌ **Don't**: Create cross-schema transactions
```java
// Bad!
@Transactional
public void createOrderAndReduceStock() {
    // Spans multiple schemas - won't work as expected with schema isolation
}
```

✅ **Do**: Use saga pattern
```java
// Good!
public void createOrder() {
    orderRepository.save(order);  // Transaction 1
    productService.reduceStock(...);  // Transaction 2 (separate)
}
```

## Conclusion

This architecture provides a pragmatic balance between:
- **Simplicity** of monoliths (single deployment, easy development)
- **Modularity** of microservices (clear boundaries, independent evolution)

It's ideal for:
- Teams transitioning from monolith to microservices
- Medium-sized applications that need structure but not full microservices
- Organizations wanting to delay microservices complexity

