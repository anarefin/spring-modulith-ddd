# Quick Reference Card

## Start the Application

```bash
# Using Docker Compose (Recommended)
docker-compose up --build

# Using convenience script
./start.sh

# Stop
docker-compose down
```

## Test the Complete Flow

```bash
# Interactive test script
./test-api.sh

# Or manual curl commands (see below)
```

## API Endpoints Cheat Sheet

### Product Module (Port 8080)

```bash
# Create Product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Product Name","description":"Description","price":99.99,"stock":100}'

# Get All Products
curl http://localhost:8080/api/products

# Get Product by ID
curl http://localhost:8080/api/products/1

# Get Available Products (stock > 0)
curl http://localhost:8080/api/products/available

# Update Product
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Name","description":"Updated","price":89.99,"stock":50}'

# Delete Product
curl -X DELETE http://localhost:8080/api/products/1
```

### Order Module (Port 8080)

```bash
# Create Order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Get All Orders
curl http://localhost:8080/api/orders

# Get Order by ID
curl http://localhost:8080/api/orders/1

# Get Orders by Status
curl http://localhost:8080/api/orders/status/PENDING
curl http://localhost:8080/api/orders/status/PAID

# Cancel Order
curl -X PUT http://localhost:8080/api/orders/1/cancel
```

### Payment Module (Port 8080)

```bash
# Process Payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"paymentMethod":"CREDIT_CARD"}'

# Get All Payments
curl http://localhost:8080/api/payments

# Get Payment by ID
curl http://localhost:8080/api/payments/1

# Get Payment by Order ID
curl http://localhost:8080/api/payments/order/1

# Get Payments by Status
curl http://localhost:8080/api/payments/status/SUCCESS
```

## Complete E-commerce Flow

```bash
# 1. View products
curl http://localhost:8080/api/products

# 2. Create order (product 1, quantity 2)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
# Note the order ID from response

# 3. Process payment (assuming order ID is 1)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"paymentMethod":"CREDIT_CARD"}'

# 4. Verify order is PAID
curl http://localhost:8080/api/orders/1

# 5. Verify product stock reduced
curl http://localhost:8080/api/products/1
```

## Database Access

```bash
# Connect to PostgreSQL container
docker exec -it modular-monolith-postgres psql -U admin -d modular_monolith_db

# List schemas
\dn

# View tables in each schema
\dt product_schema.*
\dt order_schema.*
\dt payment_schema.*

# Query data
SELECT * FROM product_schema.products;
SELECT * FROM order_schema.orders;
SELECT * FROM payment_schema.payments;

# Exit
\q
```

## Logs

```bash
# View all logs
docker-compose logs -f

# View app logs only
docker-compose logs app -f

# View postgres logs only
docker-compose logs postgres -f
```

## Troubleshooting

```bash
# Check container status
docker-compose ps

# Restart containers
docker-compose restart

# Clean rebuild
docker-compose down -v
docker system prune -a
docker-compose up --build

# Check database is ready
docker exec modular-monolith-postgres pg_isready -U admin

# View application health
curl http://localhost:8080/actuator/health
```

## Module Dependencies

```
Product → (no dependencies)
Order → depends on Product
Payment → depends on Order
```

## Inter-Module Communication Examples

### Order Module calling Product Module

```java
// In OrderServiceImpl
Product product = productService.getProductById(productId);  // ← Inter-module call
boolean hasStock = productService.hasAvailableStock(productId, quantity);  // ← Inter-module call
productService.reduceStock(productId, quantity);  // ← Inter-module call
```

### Payment Module calling Order Module

```java
// In PaymentServiceImpl
Order order = orderService.getOrderById(orderId);  // ← Inter-module call
orderService.updateOrderStatus(orderId, OrderStatus.PAID);  // ← Inter-module call
```

## Schema Verification

```sql
-- Verify schema isolation
SELECT schemaname, tablename 
FROM pg_tables 
WHERE schemaname IN ('product_schema', 'order_schema', 'payment_schema')
ORDER BY schemaname, tablename;

-- Expected output:
-- product_schema | products
-- order_schema   | orders
-- payment_schema | payments
```

## Sample Data

After startup, these products are automatically created:

| ID | Name | Price | Stock |
|----|------|-------|-------|
| 1 | Dell XPS 15 Laptop | $1,299.99 | 10 |
| 2 | Logitech MX Master 3 | $99.99 | 50 |
| 3 | Keychron K2 Keyboard | $79.99 | 30 |
| 4 | LG 27" 4K Monitor | $399.99 | 15 |
| 5 | Sony WH-1000XM5 | $349.99 | 25 |

## Common HTTP Response Codes

- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success with no response body
- `400 Bad Request` - Invalid request (e.g., insufficient stock)
- `404 Not Found` - Resource not found

## Environment Variables

```bash
# In docker-compose.yml
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/modular_monolith_db
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin123
```

## Port Mapping

- Application: `8080` → `8080`
- PostgreSQL: `5432` → `5432`

## Project Structure Quick View

```
src/main/java/com/demo/modular/
├── product/          ← Product Module
│   ├── domain/       (Product entity)
│   ├── repository/   (ProductRepository)
│   ├── service/      (ProductService interface & impl)
│   └── api/          (ProductController)
├── order/            ← Order Module (depends on Product)
│   ├── domain/       (Order entity, OrderStatus enum)
│   ├── repository/   (OrderRepository)
│   ├── service/      (OrderService interface & impl)
│   └── api/          (OrderController)
└── payment/          ← Payment Module (depends on Order)
    ├── domain/       (Payment entity, PaymentStatus enum)
    ├── repository/   (PaymentRepository)
    ├── service/      (PaymentService interface & impl)
    └── api/          (PaymentController)
```

## Key Files

- `pom.xml` - Maven dependencies (Spring Boot 3.5.6, PostgreSQL, etc.)
- `docker-compose.yml` - Container orchestration
- `Dockerfile` - Application container definition
- `application.properties` - Spring Boot configuration
- `scripts/init-schemas.sql` - Database schema initialization

## Useful Commands

```bash
# Build without running
docker-compose build

# Run in detached mode
docker-compose up -d

# View logs from last 100 lines
docker-compose logs --tail=100

# Check disk usage
docker system df

# Remove all stopped containers
docker container prune
```

