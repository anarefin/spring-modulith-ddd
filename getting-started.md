# Getting Started with Spring Modulith: A Practical Guide

## Table of Contents

1. [Introduction & Concepts](#1-introduction--concepts)
2. [Core Concepts](#2-core-concepts)
3. [Project Setup](#3-project-setup)
4. [Module Structure Deep Dive](#4-module-structure-deep-dive)
5. [Domain-Driven Design Integration](#5-domain-driven-design-integration)
6. [Inter-Module Communication](#6-inter-module-communication)
7. [Testing Strategy](#7-testing-strategy)
8. [Database Isolation](#8-database-isolation)
9. [Observability & Resilience](#9-observability--resilience)
10. [Best Practices & Patterns](#10-best-practices--patterns)

---

## 1. Introduction & Concepts

### What is Spring Modulith?

**Spring Modulith** is a framework that helps you build **modular monolithic applications** by enforcing clear boundaries between logical modules within a single Spring Boot application. It provides tooling for:

- Defining module boundaries using package structure
- Verifying module dependencies at build time
- Generating documentation automatically
- Supporting eventual module extraction to microservices

### Modular Monolith vs Microservices vs Traditional Monolith

| Aspect | Traditional Monolith | Modular Monolith | Microservices |
|--------|---------------------|------------------|---------------|
| **Deployment** | Single JAR/WAR | Single JAR/WAR | Multiple services |
| **Boundaries** | None/Informal | Enforced by framework | Network boundaries |
| **Communication** | Direct calls | Direct calls (validated) | HTTP/gRPC/messaging |
| **Complexity** | Low | Medium | High |
| **Team Autonomy** | Low | Medium-High | High |
| **Performance** | Best (in-process) | Best (in-process) | Network overhead |
| **Testing** | Simple | Module-isolated | Complex (integration) |
| **Migration** | Hard | Easy (to microservices) | N/A |

### Key Benefits

✅ **Strong Module Boundaries**: Spring Modulith enforces encapsulation at compile and runtime  
✅ **Simple Deployment**: Single artifact, no distributed system complexity  
✅ **Team Autonomy**: Teams can own modules independently  
✅ **Performance**: In-process calls, no network latency  
✅ **Easy Testing**: Test modules in isolation  
✅ **Refactoring Safety**: ArchUnit tests catch boundary violations  
✅ **Migration Path**: Extract modules to microservices when needed  
✅ **Documentation**: Auto-generated module diagrams  

### When to Choose This Architecture

**Choose Modular Monolith when:**
- Starting a new greenfield project
- Team is small to medium-sized (5-30 developers)
- Requirements are evolving and not fully understood
- You want microservices-like boundaries without operational complexity
- Performance is critical (in-process calls)
- You need a clear migration path to microservices

**Consider Microservices when:**
- Independent scaling of services is critical
- Different technology stacks per service are needed
- Teams are very large and geographically distributed
- Organizational structure aligns with service boundaries (Conway's Law)

---

## 2. Core Concepts

### Module Boundaries and Encapsulation

In Spring Modulith, a **module** is defined by:

1. **Top-level package** under the main application package
2. **@ApplicationModule annotation** in `package-info.java`
3. **Explicit public API** (what other modules can access)
4. **Internal implementation** (hidden from other modules)

**Example Module Structure:**

```
com.demo.modular.product/          ← Module root
├── api/                           ← Public API
│   ├── dto/                       ← Data Transfer Objects (PUBLIC)
│   ├── exception/                 ← Public exceptions (PUBLIC)
│   └── ProductController.java     ← REST endpoints (PUBLIC)
├── service/                       ← Public service interfaces (PUBLIC)
│   └── ProductService.java
└── internal/                      ← Implementation details (PRIVATE)
    ├── domain/                    ← Domain entities and value objects
    ├── repository/                ← Data access layer
    └── service/                   ← Service implementations
```

### Package Visibility Rules

Spring Modulith enforces that **only certain packages are accessible from other modules**:

| Package Type | Visibility | Purpose | Example |
|--------------|-----------|---------|---------|
| `api.*` | **Public** | Cross-module contracts | DTOs, exceptions, controllers |
| `service` | **Public** | Inter-module service interfaces | `ProductService` |
| `internal.*` | **Private** | Implementation details | Entities, repositories, mappers |

**Key Rule**: Other modules can **ONLY** access `api.*` and `service` packages. Everything in `internal.*` is module-private.

### Inter-Module Communication Patterns

Modules communicate through **service interfaces**:

```java
// Product Module exposes ProductService interface
package com.demo.modular.product.service;

public interface ProductService {
    Optional<ProductDTO> getProductById(Long id);
    void reduceStock(Long productId, Integer quantity);
}

// Order Module depends on ProductService
package com.demo.modular.order.internal.service;

@Service
class OrderServiceImpl implements OrderService {
    private final ProductService productService; // Inter-module dependency
    
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Call product module
        ProductDTO product = productService.getProductById(request.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(...));
        
        // Business logic using product data
        // ...
    }
}
```

### Module Metadata with @ApplicationModule

Each module declares its metadata in `package-info.java`:

```java
@ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = "product"  // Explicit dependency declaration
)
package com.demo.modular.order;
```

**Benefits:**
- Self-documenting module dependencies
- Compile-time verification of allowed dependencies
- Auto-generated module diagrams
- Runtime enforcement

### Dependency Management Between Modules

**Allowed Dependency Graph:**

```
Product Module (no dependencies)
    ↑
    │ depends on
    │
Order Module (depends on: product)
    ↑
    │ depends on
    │
Payment Module (depends on: order)
```

**Rules:**
- Dependencies must be **acyclic** (no circular dependencies)
- Dependencies must be **explicitly declared** in `@ApplicationModule`
- Private packages (`internal.*`) are **never** accessible from other modules

---

## 3. Project Setup

### Maven Configuration

#### Parent POM (`pom.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.demo</groupId>
    <artifactId>modular-monolith-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Modulith BOM -->
            <dependency>
                <groupId>org.springframework.modulith</groupId>
                <artifactId>spring-modulith-bom</artifactId>
                <version>1.2.3</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**Key Point**: Use `spring-modulith-bom` in `dependencyManagement` to ensure consistent versions.

#### Application POM (`application/pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Modulith Core Dependencies -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-actuator</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-observability</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-runtime</artifactId>
    </dependency>

    <!-- Spring Modulith Testing -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Architecture Testing -->
    <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <version>1.3.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Essential Dependencies Explained:**

| Dependency | Purpose |
|------------|---------|
| `spring-modulith-starter-core` | Core module detection and verification |
| `spring-modulith-starter-jpa` | JPA event publication support |
| `spring-modulith-actuator` | Actuator endpoints for module info |
| `spring-modulith-observability` | Tracing and observability integration |
| `spring-modulith-runtime` | Runtime module boundary enforcement |
| `spring-modulith-starter-test` | Testing utilities for module verification |

### Application Properties Configuration

**`application.properties`:**

```properties
# Application Configuration
spring.application.name=modular-monolith-poc
server.port=8080

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/modular_monolith_db
spring.datasource.username=admin
spring.datasource.password=admin123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Spring Modulith Configuration
spring.modulith.detection-strategy=explicitly-annotated

# Actuator Configuration
management.endpoints.web.exposure.include=health,info,modulith,metrics
management.endpoint.modulith.enabled=true
management.health.modulith.enabled=true

# Module Events Configuration
spring.modulith.events.enabled=true
spring.modulith.republish-outstanding-events-on-restart=true
```

**Key Configuration Explained:**

- **`spring.modulith.detection-strategy=explicitly-annotated`**: Only packages with `@ApplicationModule` are treated as modules
- **`management.endpoint.modulith.enabled=true`**: Exposes module information via `/actuator/modulith`
- **`spring.modulith.events.enabled=true`**: Enables inter-module event publication

### Spring Boot Application Setup

**`ModularMonolithApplication.java`:**

```java
package com.demo.modular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ModularMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModularMonolithApplication.class, args);
    }
}
```

**Simple and Clean**: No special configuration needed. Spring Modulith auto-configures based on package structure.

---

## 4. Module Structure Deep Dive

### Standard Module Layout

Let's build the **Product Module** step by step.

#### Step 1: Define Module with package-info.java

**`com/demo/modular/product/package-info.java`:**

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Product Module",
    allowedDependencies = {}  // No dependencies
)
package com.demo.modular.product;

/**
 * Product Module - Manages product catalog and inventory.
 * 
 * <p><b>Public API (accessible from other modules):</b></p>
 * <ul>
 *   <li>api.dto - DTOs for cross-module communication</li>
 *   <li>api.exception - Public exceptions</li>
 *   <li>service - Service interfaces for inter-module communication</li>
 * </ul>
 * 
 * <p><b>Internal Packages (module-private):</b></p>
 * <ul>
 *   <li>internal.domain - JPA entities and value objects</li>
 *   <li>internal.repository - Data access layer</li>
 *   <li>internal.service - Service implementations and mappers</li>
 * </ul>
 */
```

**Benefits:**
- Self-documenting module purpose
- Explicit dependency declaration
- Compile-time verification

#### Step 2: Public API Design - DTOs

**`com/demo/modular/product/api/dto/ProductDTO.java`:**

```java
package com.demo.modular.product.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Product entity.
 * Used for cross-module communication to avoid exposing domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have maximum 8 digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Design Principles:**
- **Validation**: Use Bean Validation annotations
- **Immutability**: Use `@Builder` for clean construction
- **Serialization**: Compatible with JSON serialization
- **Decoupling**: No reference to internal domain entities

**`com/demo/modular/product/api/dto/StockCheckDTO.java`:**

```java
package com.demo.modular.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for stock availability check results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckDTO {
    private Long productId;
    private boolean available;
    private Integer requestedQuantity;
    private Integer availableStock;
    private String message;
}
```

#### Step 3: Public API Design - Exceptions

**`com/demo/modular/product/api/exception/ProductNotFoundException.java`:**

```java
package com.demo.modular.product.api.exception;

/**
 * Thrown when a product is not found.
 * This is a public exception that other modules can catch.
 */
public class ProductNotFoundException extends RuntimeException {
    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product not found with ID: " + productId);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
```

**`com/demo/modular/product/api/exception/InsufficientStockException.java`:**

```java
package com.demo.modular.product.api.exception;

/**
 * Thrown when requested quantity exceeds available stock.
 */
public class InsufficientStockException extends RuntimeException {
    private final Long productId;
    private final Integer requested;
    private final Integer available;

    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product %d. Requested: %d, Available: %d",
            productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}
```

#### Step 4: Public Service Interface

**`com/demo/modular/product/service/ProductService.java`:**

```java
package com.demo.modular.product.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Product Service Interface - Public API for inter-module communication.
 * 
 * <p>All methods use DTOs to prevent domain model exposure to other modules.</p>
 */
public interface ProductService {
    
    ProductDTO createProduct(@Valid @NotNull ProductDTO productDTO);
    
    Optional<ProductDTO> getProductById(@NotNull Long id);
    
    List<ProductDTO> getAllProducts();
    
    List<ProductDTO> getAvailableProducts();
    
    ProductDTO updateProduct(@NotNull Long id, @Valid @NotNull ProductDTO productDTO);
    
    void deleteProduct(@NotNull Long id);
    
    /**
     * Reduces product stock by the specified quantity.
     * Called by Order module during order creation.
     */
    void reduceStock(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
    
    /**
     * Restores product stock by the specified quantity.
     * Used for compensation when order creation fails.
     */
    void restoreStock(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
    
    /**
     * Checks if product has sufficient stock for the requested quantity.
     */
    StockCheckDTO checkStockAvailability(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
}
```

**Design Principles:**
- **Interface-based**: Enables loose coupling and testability
- **Validation**: Use `@Valid` and constraint annotations
- **Documentation**: Javadoc explains inter-module usage
- **DTOs only**: No domain entities in method signatures

#### Step 5: Internal Domain Model

**`com/demo/modular/product/internal/domain/Product.java`:**

```java
package com.demo.modular.product.internal.domain;

import com.demo.modular.product.internal.domain.vo.Money;
import com.demo.modular.product.internal.domain.vo.ProductName;
import com.demo.modular.product.internal.domain.vo.Quantity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Product Aggregate Root.
 * Encapsulates product business logic and invariants.
 */
@Entity
@Table(name = "products", schema = "product_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    })
    private ProductName name;

    @Column(length = 1000)
    private String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 10, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
    })
    private Money price;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "stock", nullable = false))
    })
    private Quantity stock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Static factory method for creating new products
    public static Product create(ProductName name, String description, Money price, Quantity initialStock) {
        validateCreationParams(name, price, initialStock);
        return new Product(name, description, price, initialStock);
    }

    private Product(ProductName name, String description, Money price, Quantity initialStock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = initialStock;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateCreationParams(ProductName name, Money price, Quantity stock) {
        if (name == null) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null || !price.isPositive()) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (stock == null) {
            throw new IllegalArgumentException("Product stock is required");
        }
    }

    // Business methods
    public boolean hasAvailableStock(Quantity requestedQuantity) {
        return stock.isGreaterThanOrEqual(requestedQuantity);
    }

    public void reserveStock(Quantity quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %s, Requested: %s", stock, quantity)
            );
        }
        this.stock = this.stock.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseStock(Quantity quantity) {
        this.stock = this.stock.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePrice(Money newPrice) {
        if (!newPrice.isPositive()) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return stock.isPositive();
    }
}
```

**Design Principles:**
- **Aggregate Root**: Encapsulates business logic
- **Value Objects**: Use `Money`, `Quantity`, `ProductName` instead of primitives
- **Invariant Protection**: Business rules enforced in methods
- **Static Factory**: `create()` method ensures valid construction
- **Package-Private Constructor**: Forces use of factory method

#### Step 6: Internal Repository

**`com/demo/modular/product/internal/repository/ProductRepository.java`:**

```java
package com.demo.modular.product.internal.repository;

import com.demo.modular.product.internal.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.stock.value > 0")
    List<Product> findAvailableProducts();
}
```

**Design Principles:**
- **Package-private interface**: Not accessible from other modules
- **Spring Data JPA**: Minimal boilerplate
- **Custom queries**: For domain-specific queries

#### Step 7: Internal Service Implementation

**`com/demo/modular/product/internal/service/ProductServiceImpl.java`:**

```java
package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.*;
import com.demo.modular.product.internal.domain.Product;
import com.demo.modular.product.internal.domain.vo.*;
import com.demo.modular.product.internal.repository.ProductRepository;
import com.demo.modular.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional
class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("[Product Module] Creating product: {}", productDTO.getName());
        
        // Convert DTO to domain entity using value objects
        Product product = Product.create(
            ProductName.of(productDTO.getName()),
            productDTO.getDescription(),
            Money.of(productDTO.getPrice()),
            Quantity.of(productDTO.getStock())
        );
        
        Product saved = productRepository.save(product);
        log.info("[Product Module] Product created with id: {}", saved.getId());
        
        return productMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductById(Long id) {
        log.debug("[Product Module] Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.debug("[Product Module] Fetching all products");
        return productMapper.toDTOList(productRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        log.debug("[Product Module] Fetching available products");
        return productMapper.toDTOList(productRepository.findAvailableProducts());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reduceStock(Long productId, Integer quantity) {
        log.info("[Product Module] [Inter-Module Call] Reducing stock for product {} by {}", 
                productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        // Use domain method to enforce business rules
        product.reserveStock(Quantity.of(quantity));
        
        productRepository.save(product);
        log.info("[Product Module] Stock reduced successfully");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(Long productId, Integer quantity) {
        log.info("[Product Module] [Compensation] Restoring stock for product {} by {}", 
                productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        product.releaseStock(Quantity.of(quantity));
        
        productRepository.save(product);
        log.info("[Product Module] Stock restored successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public StockCheckDTO checkStockAvailability(Long productId, Integer quantity) {
        log.debug("[Product Module] [Inter-Module Call] Checking stock for product {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        Quantity requestedQty = Quantity.of(quantity);
        boolean available = product.hasAvailableStock(requestedQty);
        
        return StockCheckDTO.builder()
                .productId(productId)
                .available(available)
                .requestedQuantity(quantity)
                .availableStock(product.getStock().getValue())
                .message(available ? "Stock available" : "Insufficient stock")
                .build();
    }
}
```

**Design Principles:**
- **Package-private class**: Implementation hidden from other modules
- **Transaction management**: Uses Spring's `@Transactional`
- **REQUIRES_NEW propagation**: For `reduceStock` and `restoreStock` (independent transactions)
- **Logging**: Structured logging for inter-module calls
- **Domain-driven**: Delegates business logic to domain entities

#### Step 8: Internal Mapper

**`com/demo/modular/product/internal/service/ProductMapper.java`:**

```java
package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.internal.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
class ProductMapper {

    ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName().getValue())
                .description(product.getDescription())
                .price(product.getPrice().getAmount())
                .stock(product.getStock().getValue())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    List<ProductDTO> toDTOList(List<Product> products) {
        return products.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
```

**Design Principles:**
- **Package-private**: Mapper is internal implementation detail
- **No reverse mapping in examples**: DTOs converted to domain via factory methods
- **Value object unwrapping**: Extracts primitive values for DTOs

#### Step 9: REST Controller

**`com/demo/modular/product/api/ProductController.java`:**

```java
package com.demo.modular.product.api;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        log.info("REST: Creating product: {}", productDTO.getName());
        ProductDTO created = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        log.info("REST: Fetching all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("REST: Fetching product with id: {}", id);
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<ProductDTO>> getAvailableProducts() {
        log.info("REST: Fetching available products");
        return ResponseEntity.ok(productService.getAvailableProducts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("REST: Deleting product with id: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Design Principles:**
- **Public controller**: Part of module's public API
- **Depends on service interface**: Not implementation
- **Validation**: Uses `@Valid` for automatic validation
- **Standard REST conventions**: HTTP status codes, resource paths

---

## 5. Domain-Driven Design Integration

### Value Objects Pattern

**Value Objects** are immutable objects that represent domain concepts with no identity. They encapsulate validation and behavior.

#### Benefits of Value Objects

✅ **Type Safety**: `Money` instead of `BigDecimal` prevents mixing different concepts  
✅ **Validation**: Self-validating objects ensure invariants  
✅ **Immutability**: Thread-safe and prevents unexpected mutations  
✅ **Expressiveness**: Code reads like business language  
✅ **Encapsulation**: Business rules live with the data  

### Implementing Value Objects

#### Money Value Object

**`com/demo/modular/product/internal/domain/vo/Money.java`:**

```java
package com.demo.modular.product.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object representing monetary amount.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Money {
    
    private BigDecimal amount;
    private String currency;
    
    private Money(BigDecimal amount, String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    // Static factory methods
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount, "USD");
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, "USD");
    }
    
    // Business operations
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }
    
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Money amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }
    }
    
    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
    }
    
    @Override
    public String toString() {
        return currency + " " + amount;
    }
}
```

**Key Features:**
- **Immutable**: All operations return new instances
- **Self-validating**: Constructor enforces invariants
- **JPA embeddable**: Can be embedded in entities
- **Currency-aware**: Prevents mixing different currencies
- **Precision control**: Always 2 decimal places

#### Quantity Value Object

**`com/demo/modular/product/internal/domain/vo/Quantity.java`:**

```java
package com.demo.modular.product.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object representing quantity.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Quantity {
    
    private Integer value;
    
    private Quantity(Integer value) {
        validate(value);
        this.value = value;
    }
    
    public static Quantity of(Integer value) {
        return new Quantity(value);
    }
    
    public static Quantity zero() {
        return new Quantity(0);
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }
    
    public Quantity subtract(Quantity other) {
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Cannot subtract quantity resulting in negative value");
        }
        return new Quantity(result);
    }
    
    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }
    
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= other.value;
    }
    
    public boolean isPositive() {
        return value > 0;
    }
    
    public boolean isZero() {
        return value == 0;
    }
    
    private void validate(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Quantity value cannot be null");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
```

#### ProductName Value Object

**`com/demo/modular/product/internal/domain/vo/ProductName.java`:**

```java
package com.demo.modular.product.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object representing Product Name.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class ProductName {
    
    private String value;
    
    private ProductName(String value) {
        validate(value);
        this.value = value.trim();
    }
    
    public static ProductName of(String value) {
        return new ProductName(value);
    }
    
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (value.trim().length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 characters");
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Rich Domain Models vs Anemic Models

#### ❌ Anemic Model (Anti-Pattern)

```java
// Bad: Anemic model - just data, no behavior
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    
    // Only getters and setters
}

// Business logic in service layer
@Service
public class ProductService {
    public void reduceStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        product.setStock(product.getStock() - quantity);
    }
}
```

**Problems:**
- Business logic scattered in services
- Invariants not protected
- Easy to put entity in invalid state
- Hard to test business rules

#### ✅ Rich Domain Model (Recommended)

```java
// Good: Rich domain model - encapsulates business logic
public class Product {
    private ProductId id;
    private ProductName name;
    private Money price;
    private Quantity stock;
    
    // Business method enforces invariants
    public void reserveStock(Quantity quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %s, Requested: %s", 
                    stock, quantity)
            );
        }
        this.stock = this.stock.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean hasAvailableStock(Quantity requestedQuantity) {
        return stock.isGreaterThanOrEqual(requestedQuantity);
    }
}

// Service delegates to domain
@Service
class ProductServiceImpl implements ProductService {
    public void reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        // Business logic in domain model
        product.reserveStock(Quantity.of(quantity));
        
        productRepository.save(product);
    }
}
```

**Benefits:**
- Business rules in domain entities
- Invariants always protected
- Self-documenting code
- Easy to test business logic
- Type safety with value objects

### Value Object Usage Examples

```java
// Instead of primitives...
BigDecimal price = new BigDecimal("100.00");
int quantity = 5;
BigDecimal total = price.multiply(new BigDecimal(quantity));

// Use value objects for expressiveness and safety
Money unitPrice = Money.of(new BigDecimal("100.00"));
Quantity quantity = Quantity.of(5);
Money total = unitPrice.multiply(quantity.getValue());

// Enforces business rules
Money invalid = Money.of(new BigDecimal("-10.00")); // ❌ Throws exception
Quantity negative = Quantity.of(-5); // ❌ Throws exception

// Type safety prevents mistakes
void processPayment(Money amount) { /* ... */ }
void shipOrder(Quantity quantity) { /* ... */ }

// Compile-time safety
processPayment(unitPrice);     // ✅ Correct
processPayment(quantity);      // ❌ Compile error - type mismatch
```

---

## 6. Inter-Module Communication

### Service Interfaces as Module Contracts

Modules communicate through **public service interfaces**. This creates a stable contract that decouples modules.

### Example: Order Module Depending on Product Module

#### Order Module Declaration

**`com/demo/modular/order/package-info.java`:**

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = "product"  // ✅ Explicitly allows dependency on Product
)
package com.demo.modular.order;
```

#### Inter-Module Service Call

**`com/demo/modular/order/internal/service/OrderServiceImpl.java`:**

```java
package com.demo.modular.order.internal.service;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.internal.domain.Order;
import com.demo.modular.order.internal.repository.OrderRepository;
import com.demo.modular.order.service.OrderService;

// ✅ Allowed: Import from Product module's public API
import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService; // ✅ Inject Product module service
    private final OrderMapper orderMapper;

    @Override
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("[Order Module] Creating order for product {} with quantity {}", 
                request.getProductId(), request.getQuantity());
        
        Long productId = request.getProductId();
        Integer quantity = request.getQuantity();
        boolean stockReduced = false;
        
        try {
            // ✅ Inter-module call: Get product details
            ProductDTO product = productService.getProductById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            
            log.debug("[Order Module] Product found: {}, price: {}", 
                    product.getName(), product.getPrice());
            
            // ✅ Inter-module call: Check stock availability
            StockCheckDTO stockCheck = productService.checkStockAvailability(productId, quantity);
            if (!stockCheck.isAvailable()) {
                throw new InsufficientStockException(
                    productId, quantity, stockCheck.getAvailableStock());
            }
            
            // Calculate total using value objects
            Money unitPrice = Money.of(product.getPrice());
            Money totalAmount = unitPrice.multiply(quantity);
            
            // Create order aggregate
            Order order = Order.create(
                ProductId.of(productId),
                ProductName.of(product.getName()),
                Quantity.of(quantity),
                totalAmount
            );
            
            Order savedOrder = orderRepository.save(order);
            log.info("[Order Module] Order saved with id: {}", savedOrder.getId());
            
            // ✅ Inter-module call: Reduce product stock
            productService.reduceStock(productId, quantity);
            stockReduced = true;
            
            log.info("[Order Module] Order created successfully with id: {}", savedOrder.getId());
            return orderMapper.toDTO(savedOrder);
            
        } catch (Exception e) {
            log.error("[Order Module] Failed to create order", e);
            
            // ✅ Compensation: Restore stock if it was reduced
            if (stockReduced) {
                try {
                    log.warn("[Order Module] [Compensation] Restoring stock for product {}", productId);
                    productService.restoreStock(productId, quantity);
                    log.info("[Order Module] [Compensation] Stock restored successfully");
                } catch (Exception compensationEx) {
                    log.error("[Order Module] [Compensation] Failed to restore stock", compensationEx);
                }
            }
            
            throw new OrderCreationException("Failed to create order: " + e.getMessage(), e);
        }
    }
}
```

**Key Points:**
- **Dependency Injection**: `ProductService` is injected (interface, not implementation)
- **Public API only**: Uses `ProductDTO`, `ProductService`, exceptions from `api` package
- **Never accesses internal**: Cannot access `Product` entity or `ProductRepository`
- **Compensation pattern**: Restores stock if order creation fails

### Transaction Boundaries and Propagation

#### Understanding Transaction Propagation

```java
// Product Module - Independent transaction for stock reduction
@Service
class ProductServiceImpl implements ProductService {
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // ✅ New transaction
    public void reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        product.reserveStock(Quantity.of(quantity));
        productRepository.save(product);
        
        // This transaction commits independently
    }
}
```

**Why REQUIRES_NEW?**
- Stock reduction should commit even if outer transaction rolls back
- Prevents distributed transaction complexity
- Enables compensation-based recovery

#### Compensation Pattern for Stock Restore

```java
@Service
class OrderServiceImpl implements OrderService {
    
    @Override
    @Transactional // Order transaction
    public OrderDTO createOrder(CreateOrderRequest request) {
        Order order = createOrderEntity(request);
        orderRepository.save(order);
        
        try {
            // This runs in separate transaction (REQUIRES_NEW)
            productService.reduceStock(request.getProductId(), request.getQuantity());
            
            // Payment processing (might fail)
            processPayment(order);
            
        } catch (Exception e) {
            // Compensation: Restore stock in separate transaction
            productService.restoreStock(request.getProductId(), request.getQuantity());
            throw e;
        }
        
        return orderMapper.toDTO(order);
    }
}
```

**Compensation Pattern:**
1. Stock reduction commits in its own transaction
2. If subsequent operations fail, manually restore stock
3. Better than distributed 2PC (two-phase commit)
4. Eventual consistency approach

### Dependency Declaration Rules

**Valid Dependency Declarations:**

```java
// ✅ Product: No dependencies (foundational module)
@ApplicationModule(
    displayName = "Product Module",
    allowedDependencies = {}
)
package com.demo.modular.product;

// ✅ Order: Depends on Product
@ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = "product"
)
package com.demo.modular.order;

// ✅ Payment: Depends on Order
@ApplicationModule(
    displayName = "Payment Module",
    allowedDependencies = "order"
)
package com.demo.modular.payment;
```

**Invalid Dependency (Circular):**

```java
// ❌ Circular dependency - will fail verification
@ApplicationModule(
    displayName = "Product Module",
    allowedDependencies = "order"  // ❌ Creates cycle: Product → Order → Product
)
package com.demo.modular.product;
```

**Spring Modulith will detect this and fail the `ApplicationModulesTest`.**

---

## 7. Testing Strategy

### Module Verification with ApplicationModulesTest

This test verifies that module boundaries are not violated and generates documentation.

**`com/demo/modular/ApplicationModulesTest.java`:**

```java
package com.demo.modular;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Test class for verifying Spring Modulith module structure and generating documentation
 */
class ApplicationModulesTest {

    ApplicationModules modules = ApplicationModules.of(ModularMonolithApplication.class);

    @Test
    void verifyModuleStructure() {
        // Verifies that module boundaries are not violated
        // This test will fail if any module accesses internals of another module
        modules.verify();
    }

    @Test
    void writeDocumentation() throws Exception {
        // Generates PlantUML diagrams showing module structure and dependencies
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }

    @Test
    void writeModuleCanvas() throws Exception {
        // Generates module canvas documentation
        new Documenter(modules)
            .writeModuleCanvases();
    }
}
```

**What it Checks:**
- ✅ Modules only access allowed public APIs
- ✅ No access to `internal` packages from other modules
- ✅ Declared dependencies match actual dependencies
- ✅ No circular dependencies

**Generated Documentation:**

After running tests, documentation is generated in `target/spring-modulith-docs/`:

```
target/spring-modulith-docs/
├── components.puml          # Overall component diagram
├── module-product.puml      # Product module diagram
├── module-product.adoc      # Product module documentation
├── module-order.puml        # Order module diagram
├── module-order.adoc        # Order module documentation
├── module-payment.puml      # Payment module diagram
└── module-payment.adoc      # Payment module documentation
```

### Architecture Enforcement with ArchUnit

ArchUnit tests enforce architectural rules at compile time.

**`com/demo/modular/architecture/ModuleArchitectureTest.java`:**

```java
package com.demo.modular.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ModuleArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.demo.modular");
    }

    @Test
    void controllersShouldOnlyDependOnServices() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..api..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..service..", "..api..", "java..", 
                    "org.springframework..", "lombok..", "jakarta..");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldOnlyBeAccessedByServices() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..repository..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().onlyBeAccessed().byAnyPackage("..service..", "..repository..");

        rule.check(importedClasses);
    }

    @Test
    void serviceImplementationsShouldNotBePublic() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..service..")
                .and().haveSimpleNameEndingWith("ServiceImpl")
                .should().notBePublic();

        rule.check(importedClasses);
    }

    @Test
    void internalPackagesShouldNotBeAccessedFromOtherModules() {
        // Product module's internal should not be accessed from outside
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("com.demo.modular.product..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.product.internal..")
                .because("Internal packages should not be accessible from other modules");

        rule.check(importedClasses);
    }

    @Test
    void dtosShouldResideInApiPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("DTO")
                .and().resideInAnyPackage("com.demo.modular..")
                .should().resideInAnyPackage("..api.dto..");

        rule.check(importedClasses);
    }

    @Test
    void domainEntitiesShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAnyPackage("..internal.domain..")
                .because("JPA entities are internal implementation details");

        rule.check(importedClasses);
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controllers").definedBy("..api..")
                .layer("Services").definedBy("..service..", "..internal.service..")
                .layer("Repositories").definedBy("..repository..", "..internal.repository..")
                .layer("Domain").definedBy("..domain..", "..internal.domain..")
                
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Services")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services");

        rule.check(importedClasses);
    }
}
```

**Benefits:**
- Catches architecture violations early (compile-time)
- Prevents accidental coupling
- Self-documenting architecture rules
- Enforces team conventions

### Module-Specific Tests

Test individual modules in isolation.

**`com/demo/modular/product/ProductModuleTest.java`:**

```java
package com.demo.modular.product;

import com.demo.modular.ModularMonolithApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

/**
 * Test for Product module in isolation.
 * Uses Spring Modulith's @ApplicationModuleTest to test only this module.
 */
@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class ProductModuleTest {

    @Test
    void shouldCreateProduct(Scenario scenario) {
        // Test product creation in isolation
        // Only Product module and its direct dependencies are loaded
    }
}
```

**@ApplicationModuleTest Benefits:**
- Tests only the module and its direct dependencies
- Faster test execution (partial context)
- True module isolation testing
- Validates module completeness

### Testing Inter-Module Communication

**`com/demo/modular/order/OrderModuleTest.java`:**

```java
package com.demo.modular.order;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.service.OrderService;
import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderModuleTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateOrderAndReduceStock() {
        // Given: Create a product
        ProductDTO product = productService.createProduct(ProductDTO.builder()
                .name("Test Product")
                .description("Test")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .build());

        int initialStock = product.getStock();

        // When: Create an order
        OrderDTO order = orderService.createOrder(CreateOrderRequest.builder()
                .productId(product.getId())
                .quantity(2)
                .build());

        // Then: Order created and stock reduced
        assertThat(order).isNotNull();
        assertThat(order.getQuantity()).isEqualTo(2);

        ProductDTO updatedProduct = productService.getProductById(product.getId()).get();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 2);
    }
}
```

---

## 8. Database Isolation

### Schema-Per-Module Pattern

Each module owns a dedicated database schema for data isolation.

**Benefits:**
- ✅ Prevents accidental cross-module data access
- ✅ Clear ownership boundaries
- ✅ Easier migration to separate databases (microservices)
- ✅ Schema-level security possible
- ✅ Independent schema evolution

### PostgreSQL Multi-Schema Setup

#### Database Initialization Script

**`scripts/init-schemas.sql`:**

```sql
-- Create schemas for each module
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA product_schema TO admin;
GRANT ALL PRIVILEGES ON SCHEMA order_schema TO admin;
GRANT ALL PRIVILEGES ON SCHEMA payment_schema TO admin;

-- Set default schema
ALTER DATABASE modular_monolith_db SET search_path TO product_schema, order_schema, payment_schema, public;
```

#### Entity Schema Declaration

**Product Entity:**

```java
@Entity
@Table(name = "products", schema = "product_schema")
public class Product {
    // ...
}
```

**Order Entity:**

```java
@Entity
@Table(name = "orders", schema = "order_schema")
public class Order {
    // ...
}
```

**Payment Entity:**

```java
@Entity
@Table(name = "payments", schema = "payment_schema")
public class Payment {
    // ...
}
```

### Avoiding Cross-Schema Foreign Keys

**❌ Anti-Pattern: Foreign Key Across Schemas**

```sql
-- DON'T DO THIS
CREATE TABLE order_schema.orders (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product_schema.products(id) -- ❌ Tight coupling
);
```

**✅ Better: Store Reference Without FK**

```sql
-- DO THIS INSTEAD
CREATE TABLE order_schema.orders (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,  -- Reference but no FK constraint
    product_name VARCHAR(255),   -- Denormalized data
    -- No foreign key constraint
);
```

### Data Denormalization Strategies

**Example: Order Module Stores Product Snapshot**

```java
@Entity
@Table(name = "orders", schema = "order_schema")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to product (no FK)
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "product_id"))
    })
    private ProductId productId;

    // Denormalized product data (snapshot at order time)
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "product_name"))
    })
    private ProductName productName;

    @Embedded
    private Quantity quantity;

    @Embedded
    private Money totalAmount;
    
    // ...
}
```

**Benefits:**
- Order has its own product snapshot
- Product name changes don't affect historical orders
- No cross-module database queries needed
- Module independence

**Trade-offs:**
- Data duplication
- Eventual consistency
- Need synchronization mechanisms for critical updates

---

## 9. Observability & Resilience

### Spring Modulith Actuator Endpoints

Spring Modulith provides actuator endpoints for module introspection.

#### Enable Actuator Endpoints

**`application.properties`:**

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,modulith,metrics
management.endpoint.modulith.enabled=true
management.health.modulith.enabled=true
```

#### Available Endpoints

| Endpoint | Description | Example |
|----------|-------------|---------|
| `/actuator/modulith` | Module structure and dependencies | Module graph |
| `/actuator/health` | Application and module health | Overall health status |
| `/actuator/metrics` | Module-specific metrics | Method call counts |

**Example Response from `/actuator/modulith`:**

```json
{
  "modules": [
    {
      "name": "product",
      "displayName": "Product Module",
      "basePackage": "com.demo.modular.product",
      "dependencies": []
    },
    {
      "name": "order",
      "displayName": "Order Module",
      "basePackage": "com.demo.modular.order",
      "dependencies": ["product"]
    },
    {
      "name": "payment",
      "displayName": "Payment Module",
      "basePackage": "com.demo.modular.payment",
      "dependencies": ["order"]
    }
  ]
}
```

### Resilience4j Integration

Add resilience patterns for inter-module calls.

#### Dependencies

```xml
<!-- Resilience4j for Circuit Breaker, Retry, Rate Limiter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### Configuration

**`application.properties`:**

```properties
# Circuit Breaker Configuration
resilience4j.circuitbreaker.configs.default.registerHealthIndicator=true
resilience4j.circuitbreaker.configs.default.slidingWindowSize=10
resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=10s

# Circuit Breaker instances for inter-module calls
resilience4j.circuitbreaker.instances.productService.baseConfig=default
resilience4j.circuitbreaker.instances.orderService.baseConfig=default

# Retry Configuration
resilience4j.retry.configs.default.maxAttempts=3
resilience4j.retry.configs.default.waitDuration=500ms
resilience4j.retry.configs.default.enableExponentialBackoff=true
resilience4j.retry.configs.default.exponentialBackoffMultiplier=2

# Retry instances
resilience4j.retry.instances.productService.baseConfig=default
resilience4j.retry.instances.orderService.baseConfig=default

# Rate Limiter Configuration
resilience4j.ratelimiter.configs.default.limitForPeriod=50
resilience4j.ratelimiter.configs.default.limitRefreshPeriod=1s
resilience4j.ratelimiter.configs.default.timeoutDuration=100ms
```

#### Using Circuit Breaker in Service

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
class OrderServiceImpl implements OrderService {

    private final ProductService productService;

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
    @Retry(name = "productService")
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Call to product service (protected by circuit breaker)
        ProductDTO product = productService.getProductById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));
        
        // ... rest of order creation logic
    }

    // Fallback method when circuit is open
    private OrderDTO createOrderFallback(CreateOrderRequest request, Exception e) {
        log.error("Circuit breaker activated for product service", e);
        throw new OrderCreationException("Product service is currently unavailable", e);
    }
}
```

**Benefits:**
- **Circuit Breaker**: Prevents cascading failures
- **Retry**: Automatic retry with exponential backoff
- **Rate Limiter**: Protects modules from overload
- **Fallback**: Graceful degradation

### Distributed Tracing with Micrometer

#### Dependencies

```xml
<!-- Micrometer for distributed tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

#### Configuration

```properties
# Distributed Tracing Configuration
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

**Benefits:**
- Trace requests across modules
- Identify performance bottlenecks
- Debug inter-module call chains
- Monitor latency

### Monitoring Module Health

```bash
# Check overall health
curl http://localhost:8080/actuator/health

# Check circuit breakers
curl http://localhost:8080/actuator/circuitbreakers

# Check metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

---

## 10. Best Practices & Patterns

### Module Design Principles

1. **High Cohesion**: Related functionality stays within one module
2. **Low Coupling**: Modules depend on interfaces, not implementations
3. **Explicit Dependencies**: Declare dependencies in `@ApplicationModule`
4. **Acyclic Dependencies**: No circular module dependencies
5. **Public API Minimalism**: Expose only what's necessary

### Package Organization Conventions

**Standard Structure:**

```
com.demo.modular.{module}/
├── api/
│   ├── dto/              ← DTOs (public)
│   ├── exception/        ← Exceptions (public)
│   └── *Controller.java  ← REST controllers (public)
├── service/
│   └── *Service.java     ← Service interfaces (public)
├── internal/
│   ├── domain/           ← Entities, value objects (private)
│   │   └── vo/           ← Value objects
│   ├── repository/       ← Repositories (private)
│   └── service/          ← Service implementations (private)
│       ├── *ServiceImpl.java
│       └── *Mapper.java
└── package-info.java     ← Module metadata
```

### Error Handling Across Modules

#### Global Exception Handler

**`com/demo/modular/config/GlobalExceptionHandler.java`:**

```java
package com.demo.modular.config;

import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @Data
    @Builder
    static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }
}
```

### Migration Path to Microservices

When your application grows, you can extract modules to microservices.

#### Current State (Modular Monolith)

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

#### Future State (Microservices)

```
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Product │    │  Order  │    │ Payment │
│ Service │←HTTP→│ Service │←HTTP→│ Service │
└────┬────┘    └────┬────┘    └────┬────┘
     │              │              │
  ┌──┴──┐       ┌──┴──┐       ┌──┴──┐
  │ DB1 │       │ DB2 │       │ DB3 │
  └─────┘       └─────┘       └─────┘
```

#### Migration Steps

**Step 1: Already Done** ✅
- Clear module boundaries
- Separate database schemas
- Service interfaces for communication

**Step 2: Extract Module to Separate Repository**
```bash
# Create new Spring Boot project for Product Service
# Copy product module code
# Keep public API (api, service packages) identical
```

**Step 3: Replace Direct Calls with HTTP/gRPC**

**Before (Direct Call):**
```java
@Service
class OrderServiceImpl implements OrderService {
    private final ProductService productService; // Direct dependency
    
    public OrderDTO createOrder(CreateOrderRequest request) {
        ProductDTO product = productService.getProductById(request.getProductId())
            .orElseThrow(...);
        // ...
    }
}
```

**After (HTTP Call via Feign Client):**
```java
@FeignClient(name = "product-service", url = "${product.service.url}")
interface ProductServiceClient extends ProductService {
    // Same interface - implementation now uses HTTP
}

@Service
class OrderServiceImpl implements OrderService {
    private final ProductService productService; // Now a Feign client
    
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Same code - transparent to business logic
        ProductDTO product = productService.getProductById(request.getProductId())
            .orElseThrow(...);
        // ...
    }
}
```

**Key Advantage**: Business logic doesn't change, only the transport mechanism!

**Step 4: Deploy Independently**
- Product Service: Port 8081
- Order Service: Port 8082
- Payment Service: Port 8083

**Step 5: Add Service Discovery (Optional)**
- Use Eureka, Consul, or Kubernetes service discovery
- Remove hardcoded URLs

### Common Pitfalls and Solutions

| Pitfall | Problem | Solution |
|---------|---------|----------|
| **Accessing internal packages** | Other modules import `internal.*` classes | Use ArchUnit tests to catch violations |
| **Circular dependencies** | Module A depends on B, B depends on A | Extract common functionality to separate module |
| **Anemic domain models** | Business logic in services, not entities | Use rich domain models with value objects |
| **Tight coupling via DTOs** | DTOs contain too much internal detail | Design DTOs as contracts, not entity mirrors |
| **Missing compensation logic** | Stock not restored on order failure | Implement saga pattern with compensation |
| **Direct database access** | Module queries another module's schema | Use service interfaces, never cross-schema queries |
| **Public internal classes** | Internal classes marked as `public` | Make them package-private, use ArchUnit to enforce |
| **Missing module metadata** | Forgot `@ApplicationModule` annotation | Enable detection strategy: `explicitly-annotated` |

### Performance Considerations

**Benefits of Modular Monolith:**
- ✅ **In-process calls**: No network latency
- ✅ **Single transaction**: ACID across modules (if needed)
- ✅ **Shared memory**: No serialization overhead
- ✅ **Fast development**: No inter-service protocol overhead

**When to Extract to Microservices:**
- Module needs independent scaling
- Different technology stack required
- Team organization benefits from separation
- Deployment independence critical

### Summary of Key Takeaways

1. **Spring Modulith enforces boundaries** you define via packages
2. **Use `internal` packages** to hide implementation details
3. **Service interfaces** are the contracts between modules
4. **Value objects** add type safety and expressiveness
5. **Rich domain models** encapsulate business logic
6. **Schema-per-module** enables database independence
7. **ArchUnit tests** prevent architecture violations
8. **Compensation patterns** handle distributed failures
9. **Migration to microservices** is straightforward when boundaries are clear
10. **Start modular, extract when needed**

---

## Conclusion

You now have a comprehensive guide to building modular monolithic applications with Spring Modulith. This architecture provides:

- **Simplicity of a monolith** - single deployment, shared infrastructure
- **Modularity of microservices** - clear boundaries, independent evolution
- **Migration path** - easy extraction to microservices when needed

### Next Steps

1. **Clone the example project** and explore the code
2. **Run the tests** to see Spring Modulith verification in action
3. **Build your first module** following the patterns in this guide
4. **Generate documentation** using `ApplicationModulesTest`
5. **Enforce architecture** with ArchUnit tests
6. **Start simple, refactor later** - modular monolith grows with you

### Additional Resources

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Project README](README.md) - Detailed API documentation and examples

---

**Happy Modular Monolith Building! 🚀**

