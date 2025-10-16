# Multi-Module Maven Project Structure

## Overview

This is a **true multi-module Maven project** where each business domain (Product, Order, Payment) is a separate Maven module with its own `pom.xml`. This enforces stronger boundaries than package-based separation.

## Project Structure

```
modular-monolith-parent/                    ← Parent POM (aggregator)
├── pom.xml                                 ← Parent POM with <modules>
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
└── application/                            ← Spring Boot Application Module
    ├── pom.xml                             ← Aggregates all modules
    └── src/
        ├── main/java/com/demo/modular/
        │   ├── ModularMonolithApplication.java  ← @SpringBootApplication
        │   └── config/DataInitializer.java
        └── main/resources/
            └── application.properties
```

## Module Dependencies

```
┌─────────────────┐
│ product-module  │  ← No dependencies
└────────▲────────┘
         │
         │ Maven dependency
         │
┌────────┴────────┐
│  order-module   │  ← depends on product-module
└────────▲────────┘
         │
         │ Maven dependency
         │
┌────────┴────────┐
│ payment-module  │  ← depends on order-module
└─────────────────┘
         │
         │ Maven dependency
         │
┌────────┴────────┐
│   application   │  ← depends on all modules
└─────────────────┘
```

## POM Hierarchy

### Parent POM (`pom.xml`)
```xml
<groupId>com.demo</groupId>
<artifactId>modular-monolith-parent</artifactId>
<packaging>pom</packaging>

<modules>
    <module>product-module</module>
    <module>order-module</module>
    <module>payment-module</module>
    <module>application</module>
</modules>
```

### Product Module POM
```xml
<parent>
    <groupId>com.demo</groupId>
    <artifactId>modular-monolith-parent</artifactId>
</parent>

<artifactId>product-module</artifactId>
<!-- No module dependencies -->
```

### Order Module POM
```xml
<parent>
    <groupId>com.demo</groupId>
    <artifactId>modular-monolith-parent</artifactId>
</parent>

<artifactId>order-module</artifactId>

<dependencies>
    <!-- Explicit Maven dependency on product-module -->
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>product-module</artifactId>
    </dependency>
</dependencies>
```

### Payment Module POM
```xml
<parent>
    <groupId>com.demo</groupId>
    <artifactId>modular-monolith-parent</artifactId>
</parent>

<artifactId>payment-module</artifactId>

<dependencies>
    <!-- Explicit Maven dependency on order-module -->
    <dependency>
        <groupId>com.demo</groupId>
        <artifactId>order-module</artifactId>
    </dependency>
</dependencies>
```

### Application Module POM
```xml
<parent>
    <groupId>com.demo</groupId>
    <artifactId>modular-monolith-parent</artifactId>
</parent>

<artifactId>application</artifactId>

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

## Key Benefits of Multi-Module Structure

### 1. Strong Boundaries
Each module is a separate Maven artifact. You **cannot** access classes from another module without explicitly declaring it as a dependency in `pom.xml`.

```java
// In order-module, this will COMPILE ERROR if product-module is not in pom.xml:
import com.demo.modular.product.service.ProductService;
```

### 2. Explicit Dependencies
Dependencies are declared at the Maven level, not just package imports:

```xml
<!-- order-module explicitly depends on product-module -->
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>product-module</artifactId>
</dependency>
```

### 3. Independent Building
Each module can be built independently:

```bash
# Build just product module
cd product-module
mvn clean install

# Build just order module (requires product-module in local repo)
cd order-module
mvn clean install

# Build everything from parent
mvn clean install
```

### 4. Circular Dependency Prevention
Maven enforces acyclic dependencies. This will **fail to build**:

```xml
<!-- ❌ This would create circular dependency -->
product-module → order-module
order-module → product-module  ← Maven ERROR!
```

### 5. Module Versioning
Each module can potentially have its own version:

```xml
<groupId>com.demo</groupId>
<artifactId>product-module</artifactId>
<version>1.0.0</version>
```

### 6. Easier Testing
Test each module independently:

```bash
# Test only product module
mvn test -pl product-module

# Test only order module
mvn test -pl order-module
```

### 7. Clear Ownership
Each team can own a module with clear boundaries:
- **Team A**: product-module
- **Team B**: order-module
- **Team C**: payment-module

## Building the Project

### Build All Modules
```bash
# From project root
mvn clean install

# This builds in order:
# 1. product-module (no dependencies)
# 2. order-module (depends on product-module)
# 3. payment-module (depends on order-module)
# 4. application (depends on all)
```

### Build Specific Module
```bash
# Build only product module
mvn clean install -pl product-module

# Build order module and its dependencies
mvn clean install -pl order-module -am

# Build application and all dependencies
mvn clean install -pl application -am
```

### Skip Tests
```bash
mvn clean install -DskipTests
```

## Running the Application

### From Command Line
```bash
# Build all modules first
mvn clean install

# Run the application module
cd application
mvn spring-boot:run
```

### From Docker
```bash
# Docker builds all modules
docker-compose up --build
```

## IDE Support

### IntelliJ IDEA
1. Open the project root (where parent pom.xml is)
2. IntelliJ automatically detects multi-module structure
3. Each module appears as a separate module in Project Structure
4. Can run/debug from application module

### Eclipse
1. Import as "Existing Maven Project"
2. Select the root directory
3. Eclipse imports all modules
4. Each module appears in Package Explorer

### VS Code
1. Open the project root
2. Java extension detects Maven multi-module
3. Can build/run from application module

## Module Isolation Demo

### ❌ This Won't Compile (Before adding dependency)

**In order-module** without product-module dependency:
```java
package com.demo.modular.order.service;

// This import will fail to compile!
import com.demo.modular.product.service.ProductService;

public class OrderServiceImpl {
    // Compilation error: Cannot find symbol ProductService
    private ProductService productService;
}
```

### ✅ This Will Compile (After adding dependency)

**Add to order-module/pom.xml:**
```xml
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>product-module</artifactId>
</dependency>
```

**Now in order-module:**
```java
package com.demo.modular.order.service;

// Now this works!
import com.demo.modular.product.service.ProductService;

@Service
public class OrderServiceImpl {
    private final ProductService productService;
    
    // Dependency injection works across modules
    public OrderServiceImpl(ProductService productService) {
        this.productService = productService;
    }
}
```

## Comparison: Package vs Multi-Module

### Package-Based (Before)
```
single-module/
└── src/main/java/
    ├── product/      ← Just a package
    ├── order/        ← Just a package
    └── payment/      ← Just a package

❌ Any package can import any other package
❌ No Maven-level enforcement
❌ Weak boundaries
```

### Multi-Module (Now)
```
parent/
├── product-module/   ← Separate Maven module
├── order-module/     ← Separate Maven module
├── payment-module/   ← Separate Maven module
└── application/      ← Separate Maven module

✅ Must declare dependencies in pom.xml
✅ Maven enforces boundaries
✅ Strong boundaries
✅ Can build/test independently
```

## Dependency Graph

```
Application Module
      │
      ├──► Product Module (v1.0.0)
      │
      ├──► Order Module (v1.0.0)
      │         │
      │         └──► Product Module (v1.0.0)
      │
      └──► Payment Module (v1.0.0)
                  │
                  └──► Order Module (v1.0.0)
                            │
                            └──► Product Module (v1.0.0)
```

## Migration to Microservices

With multi-module structure, extracting to microservices is even easier:

### Step 1: Module Already Isolated
Each module is already a separate Maven artifact with explicit dependencies.

### Step 2: Create Separate Repository
```bash
# Extract product-module to its own repo
git subtree split -P product-module -b product-service
```

### Step 3: Add REST Client
Replace direct Java calls with REST calls:

```java
// Before (multi-module)
productService.reduceStock(productId, quantity);

// After (microservice)
restTemplate.post("http://product-service/api/products/{id}/reduce-stock", ...);
```

### Step 4: Deploy Independently
Each module becomes its own deployable service.

## Build Output

After `mvn clean install`:

```
├── product-module/target/
│   └── product-module-1.0.0-SNAPSHOT.jar
│
├── order-module/target/
│   └── order-module-1.0.0-SNAPSHOT.jar
│
├── payment-module/target/
│   └── payment-module-1.0.0-SNAPSHOT.jar
│
└── application/target/
    └── application-1.0.0-SNAPSHOT.jar  ← Executable JAR with all modules
```

## Spring Boot Component Scanning

The application module scans all packages:

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.demo.modular")
public class ModularMonolithApplication {
    // Scans:
    // - com.demo.modular.product
    // - com.demo.modular.order
    // - com.demo.modular.payment
}
```

## Troubleshooting

### Module Not Found Error
```
Could not resolve dependencies for project com.demo:order-module
```

**Solution**: Build parent first
```bash
mvn clean install
```

### Circular Dependency
```
The projects in the reactor contain a cyclic reference
```

**Solution**: Remove circular dependencies from pom.xml files

### Classes Not Found at Runtime
```
ClassNotFoundException: com.demo.modular.product.service.ProductService
```

**Solution**: Ensure application module depends on all modules in its pom.xml

## Conclusion

This **true multi-module Maven structure** provides:
- ✅ **Strong boundaries** enforced by Maven
- ✅ **Explicit dependencies** declared in pom.xml
- ✅ **Independent building** and testing
- ✅ **Circular dependency prevention**
- ✅ **Team ownership** per module
- ✅ **Clear migration path** to microservices

It's significantly stronger than package-based modularization!

