# Postman Collection Guide

## Overview

This guide explains how to use the Postman collection to test the Modular Monolithic POC.

## Quick Start

### 1. Import Collection

1. Open Postman
2. Click **Import** button
3. Select the file: `Modular-Monolith-POC.postman_collection.json`
4. Collection will be imported with all requests

### 2. Start the Application

```bash
docker-compose up --build
```

Wait for the application to start (you'll see "Started ModularMonolithApplication" in logs).

### 3. Test the APIs

#### Option A: Run Complete Flow

1. In Postman, navigate to **Complete E-Commerce Flow** folder
2. Click the **▶ Run** button next to the folder name
3. Click **Run Modular Monolith POC**
4. Watch the automated flow execute all steps

#### Option B: Manual Testing

Browse through the collection and run individual requests.

## Collection Structure

### 📁 Product Module
- **GET** Get All Products
- **GET** Get Product by ID
- **GET** Get Available Products
- **POST** Create Product
- **PUT** Update Product
- **DELETE** Delete Product

### 📁 Order Module
- **GET** Get All Orders
- **GET** Get Order by ID
- **GET** Get Orders by Status
- **POST** Create Order *(Inter-module: calls Product)*
- **PUT** Cancel Order

### 📁 Payment Module
- **GET** Get All Payments
- **GET** Get Payment by ID
- **GET** Get Payment by Order ID
- **GET** Get Payments by Status
- **POST** Process Payment *(Inter-module: calls Order)*

### 📁 Complete E-Commerce Flow
Automated 5-step flow:
1. View available products
2. Create order (Order → Product communication)
3. Process payment (Payment → Order communication)
4. Verify order status
5. Verify product stock reduced

## Variables

The collection uses variables that are automatically set:

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `base_url` | Application URL | `http://localhost:8080` |
| `product_id` | Current product ID | `1` |
| `order_id` | Current order ID | `1` |
| `payment_id` | Current payment ID | `1` |

Variables are automatically populated when running the Complete Flow.

## Example Requests

### Create Product

**Request:**
```http
POST http://localhost:8080/api/products
Content-Type: application/json

{
    "name": "New Test Product",
    "description": "A test product created via Postman",
    "price": 149.99,
    "stock": 50
}
```

**Response:**
```json
{
    "id": 6,
    "name": "New Test Product",
    "description": "A test product created via Postman",
    "price": 149.99,
    "stock": 50,
    "createdAt": "2025-10-16T10:30:00",
    "updatedAt": "2025-10-16T10:30:00"
}
```

### Create Order

**Request:**
```http
POST http://localhost:8080/api/orders
Content-Type: application/json

{
    "productId": 1,
    "quantity": 2
}
```

**Response:**
```json
{
    "id": 1,
    "productId": 1,
    "productName": "Dell XPS 15 Laptop",
    "quantity": 2,
    "totalAmount": 2599.98,
    "status": "PENDING",
    "createdAt": "2025-10-16T10:31:00",
    "updatedAt": "2025-10-16T10:31:00"
}
```

**What happens behind the scenes:**
1. ✅ Order Module calls Product Module to validate product exists
2. ✅ Order Module calls Product Module to check stock availability
3. ✅ Order is created with PENDING status
4. ✅ Order Module calls Product Module to reduce stock by 2

### Process Payment

**Request:**
```http
POST http://localhost:8080/api/payments
Content-Type: application/json

{
    "orderId": 1,
    "paymentMethod": "CREDIT_CARD"
}
```

**Response:**
```json
{
    "id": 1,
    "orderId": 1,
    "amount": 2599.98,
    "status": "SUCCESS",
    "paymentMethod": "CREDIT_CARD",
    "transactionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2025-10-16T10:32:00",
    "updatedAt": "2025-10-16T10:32:00"
}
```

**What happens behind the scenes:**
1. ✅ Payment Module calls Order Module to validate order exists
2. ✅ Checks order is in PENDING status
3. ✅ Simulates payment processing (95% success rate)
4. ✅ Payment Module calls Order Module to update status to PAID

## Testing Inter-Module Communication

### Order → Product Communication

When creating an order, watch the application logs:

```
[Order] Creating order for product 1 with quantity 2
[Order] Reducing stock for product 1 by 2
[Product] Reducing stock for product 1 by 2
[Product] Stock reduced successfully. New stock: 8
[Order] Order created successfully with id: 1
```

### Payment → Order Communication

When processing payment, watch the logs:

```
[Payment] Processing payment for order 1
[Payment] Simulating payment gateway for amount: 2599.98
[Payment] Payment processed successfully with transaction id: ...
[Payment] Updating order status to PAID
[Order] Updating order 1 status to PAID
[Order] Order status updated successfully
```

## Automated Tests

The collection includes automated tests that run after each request:

### Product Creation Test
```javascript
pm.test("Product created successfully", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.name).to.eql("New Test Product");
});
```

### Order Creation Test
```javascript
pm.test("Order created with PENDING status", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.eql("PENDING");
});
```

### Payment Test
```javascript
pm.test("Payment processed", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.be.oneOf(["SUCCESS", "FAILED"]);
});
```

## Run Collection with Newman (CLI)

You can also run the collection from command line using Newman:

```bash
# Install Newman
npm install -g newman

# Run collection
newman run Modular-Monolith-POC.postman_collection.json

# Run with HTML report
newman run Modular-Monolith-POC.postman_collection.json -r html
```

## Troubleshooting

### Connection Refused

**Problem:** `Error: connect ECONNREFUSED 127.0.0.1:8080`

**Solution:**
```bash
# Check if application is running
docker-compose ps

# If not running, start it
docker-compose up --build
```

### 404 Not Found

**Problem:** Endpoint returns 404

**Solution:**
- Check the `base_url` variable is set to `http://localhost:8080`
- Verify the endpoint path in the request
- Check application logs for errors

### 400 Bad Request

**Problem:** Order creation fails with 400

**Possible causes:**
- Product ID doesn't exist
- Insufficient stock
- Invalid quantity (must be > 0)

**Solution:** Run "Get Available Products" first to see valid product IDs

### Payment Already Processed

**Problem:** `Payment already processed for order: 1`

**Solution:** Create a new order before processing another payment

## Sample Data

The application pre-loads 5 products on startup:

| ID | Name | Price | Stock |
|----|------|-------|-------|
| 1 | Dell XPS 15 Laptop | $1,299.99 | 10 |
| 2 | Logitech MX Master 3 | $99.99 | 50 |
| 3 | Keychron K2 Keyboard | $79.99 | 30 |
| 4 | LG 27" 4K Monitor | $399.99 | 15 |
| 5 | Sony WH-1000XM5 | $349.99 | 25 |

## Order Status Flow

```
PENDING → PAID      (payment success)
PENDING → FAILED    (payment failed)
PENDING → CANCELLED (manual cancellation)
```

## Payment Status Flow

```
PENDING → SUCCESS   (payment processed successfully)
PENDING → FAILED    (payment processing failed)
SUCCESS → REFUNDED  (future feature)
```

## Tips

1. **Run Complete Flow First** - This gives you a quick overview of all modules working together

2. **Watch Application Logs** - Run `docker-compose logs -f app` to see inter-module communication

3. **Use Variables** - The collection automatically manages IDs between requests when running the Complete Flow

4. **Test Edge Cases** - Try creating orders with:
   - Invalid product IDs
   - Quantity exceeding stock
   - Zero quantity

5. **Payment Simulation** - Payment has 95% success rate, so occasionally you'll see FAILED status (this is expected)

## Advanced: Environment Variables

For testing multiple environments, create Postman environments:

### Development Environment
```json
{
  "base_url": "http://localhost:8080",
  "environment": "development"
}
```

### Production Environment
```json
{
  "base_url": "https://your-production-url.com",
  "environment": "production"
}
```

Switch environments using the dropdown in Postman's top-right corner.

## Next Steps

After testing with Postman:

1. ✅ Verify all API endpoints work
2. ✅ Confirm inter-module communication functions correctly
3. ✅ Check database schemas isolation (see README.md)
4. ✅ Review application logs for any errors
5. ✅ Try the shell script: `./test-api.sh`

## Support

For issues or questions:
- Check `README.md` for application documentation
- Review `QUICK-REFERENCE.md` for API examples
- See `ARCHITECTURE.md` for system design details

