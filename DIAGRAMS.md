# Architecture Diagrams

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Docker Compose                              │
│                                                                   │
│  ┌──────────────────────────┐    ┌─────────────────────────┐   │
│  │   PostgreSQL 17          │    │  Spring Boot App        │   │
│  │   Container              │    │  Container              │   │
│  │                          │    │                         │   │
│  │  ┌──────────────────┐   │    │  ┌──────────────────┐  │   │
│  │  │ product_schema   │   │◄───┼──┤ Product Module   │  │   │
│  │  │  - products      │   │    │  │  (Port 8080)     │  │   │
│  │  └──────────────────┘   │    │  └──────────────────┘  │   │
│  │                          │    │           ▲            │   │
│  │  ┌──────────────────┐   │    │           │            │   │
│  │  │ order_schema     │   │◄───┼──┐  ┌─────┴──────────┐ │   │
│  │  │  - orders        │   │    │  └──┤ Order Module   │ │   │
│  │  └──────────────────┘   │    │     │  (Port 8080)   │ │   │
│  │                          │    │     └────────────────┘ │   │
│  │  ┌──────────────────┐   │    │           ▲            │   │
│  │  │ payment_schema   │   │◄───┼──┐        │            │   │
│  │  │  - payments      │   │    │  │  ┌─────┴──────────┐ │   │
│  │  └──────────────────┘   │    │  └──┤ Payment Module │ │   │
│  │                          │    │     │  (Port 8080)   │ │   │
│  │  Port: 5432              │    │     └────────────────┘ │   │
│  └──────────────────────────┘    └─────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
         │                                      │
         │                                      │
    localhost:5432                        localhost:8080
         │                                      │
    [Database Tools]                       [REST Clients]
```

## Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                     No Dependencies                         │
│                           ▲                                  │
│                           │                                  │
│              ┌────────────┴──────────────┐                  │
│              │    Product Module         │                  │
│              │  (product_schema)         │                  │
│              │                           │                  │
│              │  ProductService:          │                  │
│              │  + createProduct()        │                  │
│              │  + getProductById()       │                  │
│              │  + reduceStock()          │◄────┐            │
│              │  + hasAvailableStock()    │     │            │
│              └───────────────────────────┘     │            │
│                                                 │            │
│                                          Uses   │            │
│                                                 │            │
│              ┌───────────────────────────┐     │            │
│              │    Order Module           │     │            │
│              │  (order_schema)           │─────┘            │
│              │                           │                  │
│              │  OrderService:            │                  │
│              │  + createOrder()          │                  │
│              │  + getOrderById()         │                  │
│              │  + updateOrderStatus()    │◄────┐            │
│              └───────────────────────────┘     │            │
│                                                 │            │
│                                          Uses   │            │
│                                                 │            │
│              ┌───────────────────────────┐     │            │
│              │    Payment Module         │     │            │
│              │  (payment_schema)         │─────┘            │
│              │                           │                  │
│              │  PaymentService:          │                  │
│              │  + processPayment()       │                  │
│              │  + getPaymentById()       │                  │
│              └───────────────────────────┘                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Complete E-Commerce Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    User Creates Order                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  POST /api/orders                            │
        │  { productId: 1, quantity: 2 }               │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Order Module: OrderServiceImpl               │
        │  createOrder(productId, quantity)             │
        └──────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
        ┌─────────────────┐  ┌──────────────────────┐
        │ Get Product     │  │ Check Stock          │
        │ (inter-module)  │  │ (inter-module)       │
        └─────────────────┘  └──────────────────────┘
                    │                   │
                    │    ┌──────────────┘
                    ▼    ▼
        ┌──────────────────────────────────────────────┐
        │  Product Module: ProductServiceImpl           │
        │  getProductById() / hasAvailableStock()       │
        └──────────────────────────────────────────────┘
                              │
                  ┌───────────┴────────────┐
                  │  Valid & Stock OK?     │
                  └───────────┬────────────┘
                              │ Yes
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Order Module: Create Order Entity            │
        │  status = PENDING                             │
        │  Save to order_schema.orders                  │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Reduce Stock (inter-module)                  │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Product Module: ProductServiceImpl           │
        │  reduceStock(productId, quantity)             │
        │  Update product_schema.products               │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Return Order with ID                         │
        └──────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    User Processes Payment                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  POST /api/payments                           │
        │  { orderId: 1, paymentMethod: "CREDIT_CARD" } │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Payment Module: PaymentServiceImpl           │
        │  processPayment(orderId, paymentMethod)       │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Get Order (inter-module)                     │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Order Module: OrderServiceImpl               │
        │  getOrderById(orderId)                        │
        │  Read from order_schema.orders                │
        └──────────────────────────────────────────────┘
                              │
                  ┌───────────┴────────────┐
                  │  Order Status=PENDING? │
                  └───────────┬────────────┘
                              │ Yes
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Payment Module: Create Payment Entity        │
        │  status = PENDING                             │
        │  Save to payment_schema.payments              │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Simulate Payment Gateway                     │
        │  (95% success rate)                           │
        └──────────────────────────────────────────────┘
                              │
                  ┌───────────┴────────────┐
                  │  Payment Success?      │
                  └─────┬─────────────┬────┘
                    Yes │             │ No
                        ▼             ▼
        ┌───────────────────┐  ┌──────────────────┐
        │ status = SUCCESS  │  │ status = FAILED  │
        │ transactionId = UUID│ │                  │
        └───────────────────┘  └──────────────────┘
                        │             │
                        ▼             ▼
        ┌─────────────────────────────────────────────┐
        │  Update Order Status (inter-module)         │
        └─────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Order Module: OrderServiceImpl               │
        │  updateOrderStatus(orderId, PAID/FAILED)      │
        │  Update order_schema.orders                   │
        └──────────────────────────────────────────────┘
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Return Payment Result                        │
        └──────────────────────────────────────────────┘
```

## Database Schema Structure

```
┌────────────────────────────────────────────────────────────────┐
│         PostgreSQL Database: modular_monolith_db               │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  Schema: product_schema                                   │ │
│  │                                                            │ │
│  │  Table: products                                          │ │
│  │  ┌────────────┬─────────────────┬──────────┐             │ │
│  │  │ id         │ BIGSERIAL       │ PK       │             │ │
│  │  │ name       │ VARCHAR(255)    │ NOT NULL │             │ │
│  │  │ description│ VARCHAR(1000)   │          │             │ │
│  │  │ price      │ DECIMAL(10,2)   │ NOT NULL │             │ │
│  │  │ stock      │ INTEGER         │ NOT NULL │             │ │
│  │  │ created_at │ TIMESTAMP       │ NOT NULL │             │ │
│  │  │ updated_at │ TIMESTAMP       │          │             │ │
│  │  └────────────┴─────────────────┴──────────┘             │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  Schema: order_schema                                     │ │
│  │                                                            │ │
│  │  Table: orders                                            │ │
│  │  ┌─────────────┬─────────────────┬──────────┐            │ │
│  │  │ id          │ BIGSERIAL       │ PK       │            │ │
│  │  │ product_id  │ BIGINT          │ NOT NULL │ ◄─┐       │ │
│  │  │ product_name│ VARCHAR(255)    │          │   │       │ │
│  │  │ quantity    │ INTEGER         │ NOT NULL │   │       │ │
│  │  │ total_amount│ DECIMAL(10,2)   │ NOT NULL │   │       │ │
│  │  │ status      │ VARCHAR(50)     │ NOT NULL │   │       │ │
│  │  │ created_at  │ TIMESTAMP       │ NOT NULL │   │       │ │
│  │  │ updated_at  │ TIMESTAMP       │          │   │       │ │
│  │  └─────────────┴─────────────────┴──────────┘   │       │ │
│  └───────────────────────────────────────────────────┼───────┘ │
│                                                       │         │
│  ┌────────────────────────────────────────────────────┼──────┐ │
│  │  Schema: payment_schema                            │      │ │
│  │                                                     │      │ │
│  │  Table: payments                                   │      │ │
│  │  ┌──────────────┬─────────────────┬──────────┐    │      │ │
│  │  │ id           │ BIGSERIAL       │ PK       │    │      │ │
│  │  │ order_id     │ BIGINT          │ NOT NULL │ ◄──┘      │ │
│  │  │ amount       │ DECIMAL(10,2)   │ NOT NULL │           │ │
│  │  │ status       │ VARCHAR(50)     │ NOT NULL │           │ │
│  │  │ payment_method│ VARCHAR(50)    │ NOT NULL │           │ │
│  │  │ transaction_id│ VARCHAR(255)   │          │           │ │
│  │  │ created_at   │ TIMESTAMP       │ NOT NULL │           │ │
│  │  │ updated_at   │ TIMESTAMP       │          │           │ │
│  │  └──────────────┴─────────────────┴──────────┘           │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                 │
│  Note: No FK constraints between schemas (loose coupling)      │
└────────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.demo.modular
│
├── ModularMonolithApplication.java    ← Main entry point
│
├── config/
│   └── DataInitializer.java           ← Sample data loader
│
├── product/                            ← PRODUCT MODULE
│   ├── domain/
│   │   └── Product.java               ← Entity (@Entity, schema="product_schema")
│   ├── repository/
│   │   └── ProductRepository.java     ← JPA Repository
│   ├── service/
│   │   ├── ProductService.java        ← Interface (public API)
│   │   └── ProductServiceImpl.java    ← Implementation
│   └── api/
│       └── ProductController.java     ← REST Controller
│
├── order/                              ← ORDER MODULE
│   ├── domain/
│   │   ├── Order.java                 ← Entity (@Entity, schema="order_schema")
│   │   └── OrderStatus.java           ← Enum
│   ├── repository/
│   │   └── OrderRepository.java       ← JPA Repository
│   ├── service/
│   │   ├── OrderService.java          ← Interface (public API)
│   │   └── OrderServiceImpl.java      ← Implementation (depends on ProductService)
│   └── api/
│       └── OrderController.java       ← REST Controller
│
└── payment/                            ← PAYMENT MODULE
    ├── domain/
    │   ├── Payment.java               ← Entity (@Entity, schema="payment_schema")
    │   └── PaymentStatus.java         ← Enum
    ├── repository/
    │   └── PaymentRepository.java     ← JPA Repository
    ├── service/
    │   ├── PaymentService.java        ← Interface (public API)
    │   └── PaymentServiceImpl.java    ← Implementation (depends on OrderService)
    └── api/
        └── PaymentController.java     ← REST Controller
```

## Transaction Boundaries

```
┌────────────────────────────────────────────────────────────┐
│  HTTP Request: POST /api/orders                            │
└───────────────────────┬────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │  @Transactional (order_schema)│
        │  OrderServiceImpl.createOrder()│
        └───────────┬──────────┬────────┘
                    │          │
         ┌──────────▼──────┐   │
         │ TX1: Save Order │   │
         │ (order_schema)  │   │
         │                 │   │
         │ COMMIT          │   │
         └─────────────────┘   │
                               │
                    ┌──────────▼─────────────┐
                    │ Call ProductService    │
                    └──────────┬─────────────┘
                               │
                        ┌──────▼──────────────────────┐
                        │ @Transactional              │
                        │ (product_schema)            │
                        │ ProductServiceImpl          │
                        │ .reduceStock()              │
                        └──────────┬──────────────────┘
                                   │
                        ┌──────────▼──────────┐
                        │ TX2: Update Product │
                        │ (product_schema)    │
                        │                     │
                        │ COMMIT              │
                        └─────────────────────┘

Note: Two separate transactions! Not ACID across modules.
If TX2 fails, TX1 already committed (requires compensating transaction).
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Docker Host                                                 │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Docker Network: modular-network                        │ │
│  │                                                          │ │
│  │  ┌──────────────────────┐    ┌──────────────────────┐  │ │
│  │  │  Container:           │    │  Container:          │  │ │
│  │  │  postgres             │    │  app                 │  │ │
│  │  │                       │    │                      │  │ │
│  │  │  Image: postgres:17   │    │  Image: Built from  │  │ │
│  │  │                       │    │  Dockerfile          │  │ │
│  │  │  Volumes:             │    │                      │  │ │
│  │  │  - postgres_data      │    │  Depends on:        │  │ │
│  │  │  - init-schemas.sql   │    │  - postgres         │  │ │
│  │  │                       │    │    (health check)   │  │ │
│  │  │  Health Check:        │    │                      │  │ │
│  │  │  pg_isready           │    │  Environment:       │  │ │
│  │  │                       │    │  - DATASOURCE_URL   │  │ │
│  │  │  Ports:               │    │                      │  │ │
│  │  │  5432:5432            │    │  Ports:             │  │ │
│  │  └──────────▲────────────┘    │  8080:8080          │  │ │
│  │             │                  └──────────┬───────────┘  │ │
│  │             │ JDBC                        │              │ │
│  │             └─────────────────────────────┘              │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
└─────────────────────────────────────────────────────────────┘
         │                                    │
         │                                    │
    Port 5432                            Port 8080
         │                                    │
    [DB Client]                        [HTTP Client]
```

## Communication Patterns

### Internal (Inter-Module)

```
┌──────────────┐
│ Controller   │  REST API (External)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Service Impl │  Business Logic
└──────┬───────┘
       │
       ├─────► Repository (Same Module)
       │
       └─────► Service Interface (Other Module) ◄── Direct Method Call
```

### External (Client ↔ Application)

```
[Client]
   │
   │ HTTP/REST
   │
   ▼
┌────────────────┐
│  ProductController  │  /api/products/*
│  OrderController    │  /api/orders/*
│  PaymentController  │  /api/payments/*
└────────────────┘
```

## Migration Path to Microservices

```
Current State:
┌────────────────────────────────────┐
│  Single Application (JVM)          │
│  ┌──────┐  ┌──────┐  ┌─────────┐  │
│  │Product│  │Order │  │Payment  │  │
│  │Module │──│Module│──│Module   │  │
│  └───┬───┘  └───┬──┘  └────┬────┘  │
└──────┼──────────┼──────────┼────────┘
       │          │          │
       └──────────┴──────────┴────► Single DB (3 schemas)

Step 1: Separate Databases
┌────────────────────────────────────┐
│  Single Application (JVM)          │
│  ┌──────┐  ┌──────┐  ┌─────────┐  │
│  │Product│  │Order │  │Payment  │  │
│  │Module │──│Module│──│Module   │  │
│  └───┬───┘  └───┬──┘  └────┬────┘  │
└──────┼──────────┼──────────┼────────┘
       │          │          │
       ▼          ▼          ▼
  ┌────────┐ ┌────────┐ ┌────────┐
  │ DB1    │ │ DB2    │ │ DB3    │
  └────────┘ └────────┘ └────────┘

Step 2: Extract Services
┌──────────────┐  HTTP  ┌──────────────┐  HTTP  ┌──────────────┐
│  Product     │◄───────│  Order       │◄───────│  Payment     │
│  Service     │        │  Service     │        │  Service     │
└───────┬──────┘        └──────┬───────┘        └──────┬───────┘
        │                      │                       │
        ▼                      ▼                       ▼
   ┌────────┐            ┌────────┐             ┌────────┐
   │ DB1    │            │ DB2    │             │ DB3    │
   └────────┘            └────────┘             └────────┘
```

## API Flow Example

```
User Request: Create Order for Product 1, Quantity 2

1. Client → POST /api/orders
   Body: {"productId": 1, "quantity": 2}

2. OrderController.createOrder()
   ↓
3. OrderServiceImpl.createOrder(1, 2)
   ↓
4. productService.getProductById(1)  ◄── Inter-module call
   ↓
5. ProductServiceImpl.getProductById(1)
   ↓ Query: SELECT * FROM product_schema.products WHERE id = 1
   ↓
6. Return Product(id=1, name="Laptop", price=1299.99, stock=10)
   ↓
7. productService.hasAvailableStock(1, 2)  ◄── Inter-module call
   ↓ Check: stock (10) >= quantity (2) ✓
   ↓
8. Create Order entity
   order.productId = 1
   order.productName = "Laptop"  ◄── Snapshot
   order.quantity = 2
   order.totalAmount = 2599.98
   order.status = PENDING
   ↓ INSERT INTO order_schema.orders ...
   ↓
9. productService.reduceStock(1, 2)  ◄── Inter-module call
   ↓
10. ProductServiceImpl.reduceStock(1, 2)
    ↓ UPDATE product_schema.products SET stock = stock - 2 WHERE id = 1
    ↓
11. Return Order to client
    Response: 201 Created
    Body: {"id": 1, "productId": 1, "quantity": 2, "status": "PENDING", ...}
```

## Summary

This architecture demonstrates:
- ✅ Clear module separation
- ✅ Schema-level isolation
- ✅ Direct method calls for performance
- ✅ Easy migration path to microservices
- ✅ Proper transaction boundaries
- ✅ Data denormalization strategies

