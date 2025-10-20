# Modular Monolith with Spring Modulith

A production-ready e-commerce application demonstrating **modular monolithic architecture** using **Spring Modulith**, **Domain-Driven Design (DDD)**, and **PostgreSQL 17** with schema-based isolation.

## 🏗️ Architecture Overview

This application implements a **modular monolith** pattern where business capabilities are organized into loosely-coupled modules with well-defined boundaries, enforced by Spring Modulith and ArchUnit.

### Core Modules

- **Product Module** - Product catalog and inventory management (Schema: `product_schema`)
- **Order Module** - Order creation and lifecycle management (Schema: `order_schema`)
- **Payment Module** - Payment processing and transaction handling (Schema: `payment_schema`)

### Key Architecture Principles

1. **Package-Based Modularity**: Modules are organized by packages, not separate Maven projects
2. **Spring Modulith Boundaries**: Module boundaries enforced at runtime and compile-time
3. **Internal Package Encapsulation**: `internal` packages hide implementation details
4. **Schema Isolation**: Each module owns its dedicated PostgreSQL schema
5. **Public API Design**: Modules expose only `api`, `service`, and `dto` packages
6. **Domain-Driven Design**: Rich domain models with value objects
7. **ArchUnit Testing**: Architecture rules validated in automated tests

## 📐 Module Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                                                      │
│              Product Module (No Dependencies)        │
│              • ProductService                        │
│              • ProductDTO                            │
│                                                      │
└──────────────────────┬───────────────────────────────┘
                       │
                       │ depends on
                       ▼
┌─────────────────────────────────────────────────────┐
│                                                      │
│              Order Module                            │
│              • OrderService                          │
│              • OrderDTO, OrderStatus                 │
│              • Uses: ProductService                  │
│                                                      │
└──────────────────────┬───────────────────────────────┘
                       │
                       │ depends on
                       ▼
┌─────────────────────────────────────────────────────┐
│                                                      │
│              Payment Module                          │
│              • PaymentService                        │
│              • PaymentDTO, PaymentStatus             │
│              • Uses: OrderService                    │
│                                                      │
└─────────────────────────────────────────────────────┘
```

## 🏛️ Module Structure

Each module follows a consistent structure with **public API** and **internal implementation**:

```
com.demo.modular.{module}/
├── api/                          ← Public API (accessible from other modules)
│   ├── dto/                      ← Data Transfer Objects
│   ├── exception/                ← Public exceptions
│   └── {Module}Controller.java   ← REST endpoints
│
├── service/                      ← Public service interfaces
│   └── {Module}Service.java      ← Inter-module communication contract
│
├── internal/                     ← Module-private (encapsulated)
│   ├── domain/                   ← Domain entities and value objects
│   │   ├── {Entity}.java         ← JPA entities
│   │   └── vo/                   ← Value Objects (Money, ProductId, etc.)
│   ├── repository/               ← Data access layer
│   │   └── {Module}Repository.java
│   └── service/                  ← Service implementations
│       ├── {Module}ServiceImpl.java
│       └── {Module}Mapper.java
│
└── package-info.java             ← @ApplicationModule metadata
```

### Package Visibility Rules

| Package | Visibility | Purpose |
|---------|-----------|---------|
| `api.dto` | **Public** | Cross-module data contracts |
| `api.exception` | **Public** | Module-specific exceptions |
| `service` | **Public** | Service interfaces for inter-module calls |
| `internal.domain` | **Private** | Domain entities (JPA) |
| `internal.repository` | **Private** | Data access |
| `internal.service` | **Private** | Service implementations |

**Key Benefit**: Other modules can only access `api.*` and `service` packages. All implementation details in `internal` are hidden.

## 🛠️ Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.5.6 |
| **Modularity** | Spring Modulith | 1.2.3 |
| **Database** | PostgreSQL | 17 |
| **ORM** | Spring Data JPA + Hibernate | - |
| **Resilience** | Resilience4j | 2.2.0 |
| **Architecture Testing** | ArchUnit | 1.3.0 |
| **Observability** | Micrometer + Zipkin | - |
| **Build Tool** | Maven | - |
| **Containerization** | Docker Compose | - |

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- JDK 21+ (for local development)

### Quick Start with Docker

1. **Start the application**
   ```bash
   docker-compose up --build
   ```

   This will:
   - Start PostgreSQL 17 with three schemas
   - Build the Spring Boot application
   - Initialize sample product data
   - Start the application on port 8080

2. **Verify the application is running**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Access the services**
   - Application: http://localhost:8080
   - Actuator: http://localhost:8080/actuator
   - Modulith Info: http://localhost:8080/actuator/modulith
   - Database: localhost:5432
     - Database: `modular_monolith_db`
     - Username: `admin`
     - Password: `admin123`

4. **Stop the application**
   ```bash
   docker-compose down
   ```

### Local Development Setup

#### 1. Start PostgreSQL Only
```bash
docker-compose up postgres -d
```

#### 2. Build the Application
```bash
./mvnw clean install
```

#### 3. Run the Application
```bash
cd application
../mvnw spring-boot:run
```

Or run from your IDE: `ModularMonolithApplication.java`

#### 4. Run Tests
```bash
# Run all tests including Spring Modulith and ArchUnit tests
./mvnw test

# Run only architecture tests
./mvnw test -Dtest=ModuleArchitectureTest

# Run only Spring Modulith verification
./mvnw test -Dtest=ApplicationModulesTest
```

## 📡 API Documentation

### Product Module API

#### Create Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M3",
    "description": "16-inch laptop with M3 chip",
    "price": 2499.99,
    "stock": 50
  }'
```

#### Get All Products
```bash
curl http://localhost:8080/api/products
```

#### Get Product by ID
```bash
curl http://localhost:8080/api/products/1
```

#### Get Available Products
```bash
curl http://localhost:8080/api/products/available
```

#### Update Stock
```bash
curl -X PUT http://localhost:8080/api/products/1/stock \
  -H "Content-Type: application/json" \
  -d '{"stock": 100}'
```

### Order Module API

#### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

**Inter-module Flow:**
1. Validates product exists via `ProductService`
2. Checks stock availability
3. Creates order with `PENDING` status
4. Reduces product stock atomically

#### Get All Orders
```bash
curl http://localhost:8080/api/orders
```

#### Get Order by ID
```bash
curl http://localhost:8080/api/orders/1
```

#### Get Orders by Status
```bash
curl http://localhost:8080/api/orders/status/PENDING
```

### Payment Module API

#### Process Payment
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Inter-module Flow:**
1. Validates order exists via `OrderService`
2. Checks order status is `PENDING`
3. Processes payment (simulated with 95% success rate)
4. Updates order status to `PAID` or `FAILED`

#### Get Payment by Order ID
```bash
curl http://localhost:8080/api/payments/order/1
```

#### Get All Payments
```bash
curl http://localhost:8080/api/payments
```

## 🔄 Complete E-Commerce Flow

### Full Transaction Example

```bash
# Step 1: View available products
curl http://localhost:8080/api/products/available

# Step 2: Create an order (saves orderId from response)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 1}'
# Response: {"id": 1, "status": "PENDING", ...}

# Step 3: Process payment for the order
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1, "paymentMethod": "CREDIT_CARD"}'

# Step 4: Verify order status changed to PAID
curl http://localhost:8080/api/orders/1

# Step 5: Confirm product stock was reduced
curl http://localhost:8080/api/products/1
```

## 🧪 Testing & Verification

### Spring Modulith Module Verification

```bash
./mvnw test -Dtest=ApplicationModulesTest
```

This test:
- ✅ Verifies module boundaries are not violated
- ✅ Ensures `internal` packages are not accessed from other modules
- ✅ Generates PlantUML diagrams in `target/spring-modulith-docs/`

### ArchUnit Architecture Tests

```bash
./mvnw test -Dtest=ModuleArchitectureTest
```

Over 20 architecture rules enforced:
- ✅ Controllers only depend on services
- ✅ Services don't depend on controllers
- ✅ Repositories only accessed by services
- ✅ Internal packages not public
- ✅ No cyclic dependencies
- ✅ Domain entities in `internal.domain`
- ✅ DTOs in `api.dto`
- ✅ Exceptions in `api.exception`
- ✅ And many more...

### Module-Specific Tests

```bash
# Test Product module
./mvnw test -Dtest=ProductModuleTest

# Test Order module
./mvnw test -Dtest=OrderModuleTest
```

## 📊 Observability & Monitoring

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Module information
curl http://localhost:8080/actuator/modulith

# Circuit breakers
curl http://localhost:8080/actuator/circuitbreakers

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Resilience4j Configuration

The application includes:
- **Circuit Breaker**: Prevents cascading failures in inter-module calls
- **Retry**: Automatic retry with exponential backoff
- **Rate Limiter**: Protects modules from overload
- **Time Limiter**: Timeout protection

Configuration in `application.properties`:
```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.productService.baseConfig=default
resilience4j.circuitbreaker.instances.orderService.baseConfig=default

# Retry
resilience4j.retry.instances.productService.maxAttempts=3
resilience4j.retry.instances.orderService.maxAttempts=2
```

### Distributed Tracing

View traces at: http://localhost:9411 (when Zipkin is configured)

## 🗄️ Database Architecture

### Schema Isolation

```sql
-- Three separate schemas in one database
modular_monolith_db
├── product_schema
│   └── products (id, name, description, price, stock, created_at, updated_at)
├── order_schema
│   └── orders (id, product_id, product_name, quantity, total_amount, status, created_at, updated_at)
└── payment_schema
    └── payments (id, order_id, amount, status, payment_method, transaction_id, created_at, updated_at)
```

### Database Commands

```bash
# Connect to PostgreSQL
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
```

### Key Design Decisions

1. **No Foreign Keys Across Schemas**: Loose coupling between modules
2. **Data Denormalization**: `orders` table stores `product_name` snapshot
3. **Eventual Consistency**: Each module manages its own transactions
4. **Schema-Level Isolation**: Prevents accidental cross-module joins

## 🏆 Spring Modulith Features

### Module Metadata (`package-info.java`)

```java
@ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = "product"  // Explicit dependency declaration
)
package com.demo.modular.order;
```

### Benefits

1. **Compile-Time Verification**: Module boundaries checked during build
2. **Runtime Enforcement**: Spring prevents invalid module access
3. **Documentation**: Auto-generates module diagrams
4. **Testing**: Built-in module testing support
5. **Future-Proof**: Easy migration path to microservices

### Generated Documentation

After running tests, view generated docs:
```
application/target/spring-modulith-docs/
├── components.puml          # Overall component diagram
├── module-product.puml      # Product module diagram
├── module-order.puml        # Order module diagram
├── module-payment.puml      # Payment module diagram
└── *.adoc                   # AsciiDoc documentation
```

## 🎯 Domain-Driven Design (DDD)

### Value Objects

Each module includes value objects for type safety and domain expressiveness:

**Product Module:**
- `ProductId` - Type-safe product identifier
- `ProductName` - Validated product name
- `Quantity` - Ensures positive quantities
- `Money` - Handles currency and precision

**Example Usage:**
```java
// Instead of primitives:
// Long id; String name; int quantity; BigDecimal price;

// Type-safe value objects:
ProductId productId;
ProductName productName;
Quantity quantity;
Money price;
```

### Rich Domain Models

Entities contain business logic, not just data:

```java
public class Product {
    private ProductId id;
    private ProductName name;
    private Money price;
    private Quantity stock;
    
    // Business logic in entity
    public void reduceStock(Quantity quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new InsufficientStockException();
        }
        this.stock = this.stock.subtract(quantity);
    }
}
```

## 📦 Build & Deployment

### Maven Build

```bash
# Clean build
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Build specific module
cd application && mvn clean package

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Build

```bash
# Build image manually
docker build -t modular-monolith:latest .

# Run container
docker run -p 8080:8080 \
  --network modular-network \
  modular-monolith:latest
```

## 🔍 Architecture Benefits

| Benefit | Description |
|---------|-------------|
| **Strong Boundaries** | Spring Modulith enforces module encapsulation |
| **Easy Testing** | Test modules independently with clear contracts |
| **Team Autonomy** | Teams can work on separate modules concurrently |
| **Refactoring Safety** | ArchUnit tests prevent architecture violations |
| **Performance** | Direct method calls (no network overhead) |
| **Deployment Simplicity** | Single deployable artifact |
| **Migration Path** | Clear path to microservices when needed |
| **Type Safety** | Value objects prevent primitive obsession |
| **Domain Focus** | DDD patterns keep business logic central |

## 🚧 Migration Path to Microservices

This architecture provides a **seamless migration path**:

### Current State (Modular Monolith)
```
┌─────────────────────────────────┐
│   Single Spring Boot App        │
│  ┌────────┐  ┌────────┐         │
│  │Product │→ │ Order  │→ Payment│
│  └────────┘  └────────┘         │
└─────────────────────────────────┘
         ↓
    Single Database (3 schemas)
```

### Future State (Microservices)
```
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Product │    │  Order  │    │ Payment │
│ Service │ ← HTTP → │ Service │ ← HTTP →│ Service │
└────┬────┘    └────┬────┘    └────┬────┘
     │              │              │
  ┌──┴──┐       ┌──┴──┐       ┌──┴──┐
  │ DB1 │       │ DB2 │       │ DB3 │
  └─────┘       └─────┘       └─────┘
```

### Migration Steps

1. **Already Done**: Modules with clear boundaries ✅
2. **Already Done**: Separate database schemas ✅
3. **Extract Module**: Move module to separate repository
4. **Replace Calls**: Change direct calls to HTTP/gRPC
5. **Deploy Independently**: Each module as separate service

## 📚 Additional Documentation

- **[DIAGRAMS.md](DIAGRAMS.md)** - Visual architecture diagrams
- **[Modular-Monolith-POC.postman_collection.json](Modular-Monolith-POC.postman_collection.json)** - Postman collection for testing

## 🐛 Troubleshooting

### Docker Issues

```bash
# Clean Docker environment
docker-compose down -v
docker system prune -a --volumes
docker-compose up --build
```

### Database Connection Issues

```bash
# Check PostgreSQL status
docker-compose ps postgres
docker-compose logs postgres

# Verify database connectivity
docker exec -it modular-monolith-postgres pg_isready -U admin
```

### Application Logs

```bash
# View live logs
docker-compose logs -f app

# View specific module logs
docker-compose logs app | grep "com.demo.modular.order"
```

### Module Boundary Violations

If `ApplicationModulesTest` fails:
1. Check which module is accessing internal packages
2. Review the test output for specific violations
3. Ensure you're only using `api.*` and `service` packages

### ArchUnit Test Failures

If `ModuleArchitectureTest` fails:
1. Review the specific rule that failed
2. Check package structure matches conventions
3. Ensure internal classes are package-private

## 🤝 Contributing

When adding new features:

1. **Follow Module Structure**: Use `api`, `service`, and `internal` packages
2. **Add Value Objects**: Create value objects for domain primitives
3. **Write Tests**: Include unit tests and architecture tests
4. **Update Documentation**: Keep README and diagrams current
5. **Verify Module Boundaries**: Run `ApplicationModulesTest`
6. **Check Architecture**: Run `ModuleArchitectureTest`

## 📄 License

This is a proof-of-concept project demonstrating modular monolithic architecture with Spring Modulith.

## 👨‍💻 Author

Built to demonstrate best practices in:
- Modular Monolithic Architecture
- Spring Modulith
- Domain-Driven Design
- Architecture Testing
- Database Schema Isolation

---

**🌟 Key Takeaway**: This architecture gives you the simplicity of a monolith with the modularity of microservices, providing a clear evolution path as your application grows.
