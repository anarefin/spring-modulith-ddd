# Modular Monolithic Architecture POC

A proof-of-concept e-commerce application demonstrating **true multi-module Maven architecture** with PostgreSQL 17, running in Docker containers.

## Architecture Overview

This POC demonstrates a **true multi-module Maven project** where each business module is an independent Maven module with explicit dependencies:

- **Product Module** (`product-module/`) - Product catalog management (Schema: `product_schema`)
- **Order Module** (`order-module/`) - Order creation and management (Schema: `order_schema`) - **Depends on Product Module**
- **Payment Module** (`payment-module/`) - Payment processing (Schema: `payment_schema`) - **Depends on Order Module**
- **Application Module** (`application/`) - Spring Boot application that aggregates all modules

### Key Design Principles

1. **True Maven Multi-Module**: Each module has its own `pom.xml` and can be built independently
2. **Maven-Level Dependencies**: Explicit dependencies declared in `pom.xml` (not just package imports)
3. **Schema isolation**: Each module owns its dedicated PostgreSQL schema
4. **Service interfaces**: Modules expose interfaces for inter-module communication
5. **Direct method calls**: No HTTP/REST between modules - only direct Java method invocation
6. **Strong boundaries**: Maven enforces that you cannot use classes from other modules without declaring the dependency

### Inter-Module Communication Flow

```
Product Module ← Order Module ← Payment Module ← Application Module
(no dependencies)   (depends on Product)   (depends on Order)   (aggregates all)
```

- **Order → Product**: Validates product availability and reduces stock
- **Payment → Order**: Validates order and updates order status

## Technology Stack

- **JDK**: 25
- **Spring Boot**: 3.5.6
- **Database**: PostgreSQL 17 (single database, multiple schemas)
- **Build Tool**: Maven (multi-module project)
- **Containerization**: Docker & Docker Compose

## Project Structure

```
modular-monolith-parent/                    ← Parent POM (aggregator)
├── pom.xml                                 ← Parent POM defining modules
│
├── product-module/                         ← Independent Maven Module
│   ├── pom.xml                             ← Module POM (no dependencies)
│   └── src/main/java/com/demo/modular/product/
│       ├── domain/Product.java
│       ├── repository/ProductRepository.java
│       ├── service/ProductService.java
│       ├── service/ProductServiceImpl.java
│       └── api/ProductController.java
│
├── order-module/                           ← Independent Maven Module
│   ├── pom.xml                             ← Module POM (depends on product-module)
│   └── src/main/java/com/demo/modular/order/
│       ├── domain/Order.java
│       ├── domain/OrderStatus.java
│       ├── repository/OrderRepository.java
│       ├── service/OrderService.java
│       ├── service/OrderServiceImpl.java
│       └── api/OrderController.java
│
├── payment-module/                         ← Independent Maven Module
│   ├── pom.xml                             ← Module POM (depends on order-module)
│   └── src/main/java/com/demo/modular/payment/
│       ├── domain/Payment.java
│       ├── domain/PaymentStatus.java
│       ├── repository/PaymentRepository.java
│       ├── service/PaymentService.java
│       ├── service/PaymentServiceImpl.java
│       └── api/PaymentController.java
│
├── application/                            ← Spring Boot Application Module
│   ├── pom.xml                             ← Aggregates all modules
│   └── src/
│       ├── main/java/com/demo/modular/
│       │   ├── ModularMonolithApplication.java  ← @SpringBootApplication
│       │   └── config/DataInitializer.java
│       └── main/resources/
│           └── application.properties
│
├── docker-compose.yml
├── Dockerfile
└── scripts/
    └── init-schemas.sql
```

## Getting Started

### Prerequisites

- Docker
- Docker Compose

### Running the Application

1. **Clone the repository**
   ```bash
   cd multi-module-demo
   ```

2. **Start the application with Docker Compose**
   ```bash
   docker-compose up --build
   ```

   This will:
   - Start PostgreSQL 17 container
   - Create three schemas (product_schema, order_schema, payment_schema)
   - Build all Maven modules
   - Build and start the Spring Boot application
   - Initialize sample product data

3. **Access the application**
   - Application: http://localhost:8080
   - PostgreSQL: localhost:5432
     - Database: `modular_monolith_db`
     - Username: `admin`
     - Password: `admin123`

4. **Stop the application**
   ```bash
   docker-compose down
   ```

### Building Locally

#### Build All Modules
```bash
./mvnw clean install
```

This builds modules in order:
1. Product Module (no dependencies)
2. Order Module (depends on Product)
3. Payment Module (depends on Order)  
4. Application Module (depends on all)

#### Build Specific Module
```bash
# Build only product module
./mvnw clean install -pl product-module

# Build order module and its dependencies
./mvnw clean install -pl order-module -am

# Build application and all dependencies
./mvnw clean install -pl application -am
```

#### Run Locally
```bash
# Build all modules first
./mvnw clean install

# Run the application module
cd application
mvn spring-boot:run
```

Or run from IDE: Run `ModularMonolithApplication.java`

## API Documentation

### Product Module API

#### Create Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "description": "Product description",
    "price": 99.99,
    "stock": 100
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

#### Get Available Products (stock > 0)
```bash
curl http://localhost:8080/api/products/available
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

This will:
1. Validate product exists (inter-module call to Product module)
2. Check stock availability (inter-module call to Product module)
3. Create order with PENDING status
4. Reduce product stock (inter-module call to Product module)

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

This will:
1. Validate order exists (inter-module call to Order module)
2. Check order is in PENDING status
3. Process payment (simulated with 95% success rate)
4. Update order status to PAID or FAILED (inter-module call to Order module)

#### Get All Payments
```bash
curl http://localhost:8080/api/payments
```

#### Get Payment by Order ID
```bash
curl http://localhost:8080/api/payments/order/1
```

## Complete E-Commerce Flow Example

### Step 1: View Available Products
```bash
curl http://localhost:8080/api/products/available
```

### Step 2: Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1
  }'
```
Response will include `orderId`.

### Step 3: Process Payment for the Order
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Step 4: Verify Order Status Changed to PAID
```bash
curl http://localhost:8080/api/orders/1
```

### Step 5: Check Product Stock Reduced
```bash
curl http://localhost:8080/api/products/1
```

## Testing with Postman

We've provided a comprehensive Postman collection for easy API testing:

### Quick Start with Postman

1. **Import Collection**
   ```
   - Open Postman
   - Click "Import"
   - Select file: Modular-Monolith-POC.postman_collection.json
   ```

2. **Run Automated Flow**
   ```
   - Navigate to "Complete E-Commerce Flow" folder
   - Click "Run" button
   - Watch all 5 steps execute automatically
   ```

3. **View Detailed Guide**
   - See `POSTMAN-GUIDE.md` for comprehensive documentation

### Collection Features

- ✅ **16 API endpoints** organized by module (Product, Order, Payment)
- ✅ **Automated test scripts** verifying responses and business logic
- ✅ **Complete e-commerce flow** (5 automated steps)
- ✅ **Environment variables** auto-managed between requests
- ✅ **Request/response examples** with detailed descriptions
- ✅ **Inter-module communication** verification with logs

### What the Collection Tests

The collection includes automated tests that verify:
- ✅ Product creation and stock management
- ✅ Order creation with Product module validation (inter-module call)
- ✅ Payment processing with Order module status update (inter-module call)
- ✅ Complete flow from product selection to successful payment
- ✅ Data consistency across all three schemas

### Run from Command Line (Newman)

```bash
# Install Newman
npm install -g newman

# Run collection
newman run Modular-Monolith-POC.postman_collection.json

# Run with HTML report
newman run Modular-Monolith-POC.postman_collection.json -r html
```

See `POSTMAN-GUIDE.md` for detailed documentation.

## Multi-Module Maven Architecture Benefits

### 1. Strong Boundaries Enforced by Maven
You **cannot** import classes from another module without declaring it in `pom.xml`:

```java
// In order-module, this will COMPILE ERROR without dependency:
import com.demo.modular.product.service.ProductService;  // ❌ Compilation error!
```

After adding to `order-module/pom.xml`:
```xml
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>product-module</artifactId>
</dependency>
```

Now it works:
```java
import com.demo.modular.product.service.ProductService;  // ✅ Works!
```

### 2. Independent Building
Each module can be built independently:

```bash
# Build just product module
cd product-module
mvn clean install

# Build just order module (requires product-module in local repo)
cd order-module
mvn clean install
```

### 3. Circular Dependency Prevention
Maven enforces acyclic dependencies:

```
product-module → order-module → payment-module  ✅ Valid
payment-module → product-module  ❌ Would create cycle - Maven ERROR!
```

### 4. Module Versioning
Each module is a separate artifact:

```
com.demo:product-module:1.0.0-SNAPSHOT
com.demo:order-module:1.0.0-SNAPSHOT
com.demo:payment-module:1.0.0-SNAPSHOT
```

### 5. Clear Team Ownership
- **Team A** owns `product-module/`
- **Team B** owns `order-module/`
- **Team C** owns `payment-module/`

Each team can work in their module with clear boundaries.

## Module Dependencies in POM

### Product Module (`product-module/pom.xml`)
```xml
<!-- No module dependencies -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

### Order Module (`order-module/pom.xml`)
```xml
<dependencies>
    <!-- Explicit dependency on Product Module -->
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>product-module</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

### Payment Module (`payment-module/pom.xml`)
```xml
<dependencies>
    <!-- Explicit dependency on Order Module -->
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>order-module</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

### Application Module (`application/pom.xml`)
```xml
<dependencies>
    <!-- Aggregates all modules -->
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>product-module</artifactId>
    </dependency>
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>order-module</artifactId>
    </dependency>
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>payment-module</artifactId>
    </dependency>
</dependencies>
```

## Module Isolation Verification

### Maven Build Order
```bash
./mvnw clean install
```

Output shows correct build order:
```
[INFO] Reactor Build Order:
[INFO] 
[INFO] Modular Monolithic POC - Parent                  [pom]
[INFO] Product Module                                   [jar]
[INFO] Order Module                                     [jar]
[INFO] Payment Module                                   [jar]
[INFO] Application Module                               [jar]
```

### Database Schema Verification
```bash
# Connect to PostgreSQL
docker exec -it modular-monolith-postgres psql -U admin -d modular_monolith_db

# List all schemas
\dn

# View tables in each schema
\dt product_schema.*
\dt order_schema.*
\dt payment_schema.*

# Query from specific schema
SELECT * FROM product_schema.products;
SELECT * FROM order_schema.orders;
SELECT * FROM payment_schema.payments;
```

## Key Observations

1. **Maven Module Isolation**: Each module is a separate Maven artifact with its own `pom.xml`
2. **Explicit Dependencies**: Dependencies are declared at Maven level in `pom.xml`
3. **Schema Isolation**: Each module's tables are isolated in separate schemas
4. **No Foreign Keys Across Modules**: Order table stores `productId` but no FK constraint
5. **Service-Level Communication**: Modules communicate through service interfaces, not database joins
6. **Single Transaction Per Module**: Each module manages its own transactions
7. **Data Denormalization**: Order stores `productName` snapshot to avoid dependencies

## Migration Path to Microservices

This architecture provides a clear migration path:

1. **Already separated at Maven level**: Each module is already a separate Maven artifact
2. **Service interfaces define clear boundaries**: Easy to replace with REST/gRPC
3. **Separate schemas**: Can easily move to separate databases
4. **To extract a module**:
   - Create separate Git repository for the module
   - Move module's schema to separate database
   - Replace direct method calls with REST/gRPC
   - Deploy module independently
   - Update dependent modules to call via HTTP

## Architecture Benefits

- ✅ **True Modular Architecture**: Maven-enforced boundaries (not just packages)
- ✅ **Strong Dependency Management**: Explicit dependencies in pom.xml
- ✅ **Independent Building**: Each module can be built separately
- ✅ **Circular Dependency Prevention**: Maven enforces acyclic dependencies
- ✅ **Schema Isolation**: Clear data boundaries
- ✅ **Performance**: Direct method calls, no network overhead
- ✅ **Flexibility**: Easy to extract modules to microservices later
- ✅ **Development Speed**: Faster than microservices for small teams
- ✅ **Easier Testing**: Can test modules independently

## Documentation

- **README.md** - This file (getting started and API docs)
- **MULTI-MODULE-STRUCTURE.md** - Detailed multi-module Maven architecture explanation
- **ARCHITECTURE.md** - Deep architectural insights and patterns
- **DIAGRAMS.md** - Visual diagrams of system architecture
- **POSTMAN-GUIDE.md** - Comprehensive Postman collection usage guide
- **QUICK-REFERENCE.md** - Command cheat sheet

## Troubleshooting

### Docker Issues

If you encounter Docker build issues:
```bash
docker-compose down -v
docker system prune -a
docker-compose up --build
```

### Maven Build Issues

If modules fail to compile:
```bash
# Clean all modules
./mvnw clean

# Rebuild from parent
./mvnw clean install
```

### Database Connection Issues

Check PostgreSQL is running:
```bash
docker-compose ps
docker-compose logs postgres
```

### Application Logs

View application logs:
```bash
docker-compose logs app -f
```

## Author

Built as a proof-of-concept for **true multi-module Maven architecture** demonstrating modular monolithic patterns.
