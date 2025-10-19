# Spring Modulith Integration

## Overview

This project has been migrated from a multi-module Maven structure to a single Spring Boot application using **Spring Modulith** for module management, boundary enforcement, and observability.

## Module Structure

The application consists of three modules organized as package-based modules:

### 1. Product Module (`com.demo.modular.product`)
- **Display Name**: Product Module
- **Dependencies**: None
- **Type**: OPEN (all packages accessible)
- **Packages**:
  - `api`: REST controllers
  - `domain`: Product entity
  - `repository`: JPA repository
  - `service`: Business logic

### 2. Order Module (`com.demo.modular.order`)
- **Display Name**: Order Module
- **Dependencies**: product
- **Type**: OPEN (all packages accessible)
- **Packages**:
  - `api`: REST controllers
  - `domain`: Order entity and OrderStatus enum
  - `repository`: JPA repository
  - `service`: Business logic with inter-module calls to ProductService

### 3. Payment Module (`com.demo.modular.payment`)
- **Display Name**: Payment Module
- **Dependencies**: order
- **Type**: OPEN (all packages accessible)
- **Packages**:
  - `api`: REST controllers
  - `domain`: Payment entity and PaymentStatus enum
  - `repository`: JPA repository
  - `service`: Business logic with inter-module calls to OrderService

## Module Dependency Graph

```
┌─────────────┐
│   Product   │ ◄── No dependencies
└──────┬──────┘
       │
       │ (service calls)
       │
┌──────▼──────┐
│    Order    │ ◄── Depends on Product
└──────┬──────┘
       │
       │ (service calls)
       │
┌──────▼──────┐
│   Payment   │ ◄── Depends on Order
└─────────────┘
```

## Spring Modulith Features Implemented

### 1. Module Verification
**Compile-time and runtime boundary checks**

Run tests to verify module structure:
```bash
./mvnw test
```

The `ApplicationModulesTest.verifyModuleStructure()` test ensures:
- Modules only depend on explicitly allowed modules
- No circular dependencies
- Module boundaries are respected

### 2. Auto-Generated Documentation
**PlantUML diagrams and module canvases**

Documentation is automatically generated during tests:
```bash
./mvnw test
```

Generated files location: `application/target/spring-modulith-docs/`

Files include:
- `components.puml` - Overall component diagram
- `module-product.puml` - Product module diagram
- `module-order.puml` - Order module diagram
- `module-payment.puml` - Payment module diagram
- `*.adoc` - AsciiDoc module canvases

To view PlantUML diagrams, use a PlantUML viewer or plugin.

### 3. Observability & Actuator Integration
**Runtime module monitoring**

The application exposes Spring Modulith metrics via Spring Boot Actuator.

**Available Endpoints:**
- `/actuator/health` - Application health (includes module health)
- `/actuator/modulith` - Module structure and information
- `/actuator/metrics` - Module-specific metrics

**Access endpoints:**
```bash
# Start the application
./mvnw spring-boot:run

# Check module information
curl http://localhost:8080/actuator/modulith
```

### 4. Module Events (Prepared for Future Use)
**Event-driven architecture support**

Spring Modulith's event publication registry is enabled:
```properties
spring.modulith.events.enabled=true
spring.modulith.republish-outstanding-events-on-restart=true
```

This prepares the application for future migration to event-based inter-module communication.

## Configuration

### Application Properties
Location: `application/src/main/resources/application.properties`

```properties
# Spring Modulith Configuration
spring.modulith.detection-strategy=explicitly-annotated

# Actuator Configuration for Modulith
management.endpoints.web.exposure.include=health,info,modulith,metrics
management.endpoint.modulith.enabled=true
management.endpoint.health.show-details=always
management.health.modulith.enabled=true

# Module Events Configuration (for future use)
spring.modulith.events.enabled=true
spring.modulith.republish-outstanding-events-on-restart=true
```

### Module Definitions
Each module is defined via `package-info.java`:

**Example - Product Module:**
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Product Module",
    allowedDependencies = {},
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.demo.modular.product;
```

## Testing

### Running Module Verification Tests
```bash
./mvnw test
```

### Test Classes
- `ApplicationModulesTest` - Verifies module structure and generates documentation
  - `verifyModuleStructure()` - Validates module boundaries
  - `writeDocumentation()` - Generates PlantUML diagrams
  - `writeModuleCanvas()` - Generates module canvases

## Build and Run

### Build Project
```bash
./mvnw clean install
```

### Run Application
```bash
./mvnw spring-boot:run
```

Or run the JAR:
```bash
java -jar application/target/application-1.0.0-SNAPSHOT.jar
```

### Run with Docker
```bash
docker-compose up
```

## Benefits of Spring Modulith

1. **Module Verification**: Compile-time checks prevent boundary violations
2. **Living Documentation**: Auto-generated diagrams always reflect current state
3. **Observability**: Built-in monitoring and health checks for modules
4. **Future-Ready**: Easy migration path to microservices or event-driven architecture
5. **Developer Experience**: Clear module boundaries improve code organization
6. **No Build Complexity**: Single Maven module simplifies build process

## Migration from Multi-Module Maven

### What Changed
✅ **Before**: 4 separate Maven modules (product-module, order-module, payment-module, application)  
✅ **After**: Single Spring Boot module with package-based modules

### What Stayed the Same
- Database schema isolation (product_schema, order_schema, payment_schema)
- REST API endpoints
- Service interfaces and implementation
- Inter-module synchronous communication
- Transaction boundaries

### Key Improvements
- Faster builds (single module)
- Simplified dependency management
- Automated boundary enforcement
- Auto-generated documentation
- Built-in observability

## Troubleshooting

### Module Verification Fails
If tests fail with boundary violations:
1. Check module dependencies in `package-info.java`
2. Ensure modules only import from allowed dependencies
3. Consider using DTOs instead of exposing domain objects

### Documentation Not Generated
1. Ensure test runs completely: `./mvnw clean test`
2. Check `application/target/spring-modulith-docs/` directory
3. Verify Spring Modulith dependencies are included

### Actuator Endpoints Not Accessible
1. Ensure application is running
2. Check `management.endpoints.web.exposure.include` in `application.properties`
3. Verify actuator dependency is present in `pom.xml`

## Further Reading

- [Spring Modulith Documentation](https://docs.spring.io/spring-modulith/reference/)
- [Spring Modulith GitHub](https://github.com/spring-projects/spring-modulith)
- [Modular Monolith Pattern](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer)

## Version Information

- **Spring Boot**: 3.5.6
- **Spring Modulith**: 1.2.3
- **Java**: 21
- **Build Tool**: Maven 3.9.9

