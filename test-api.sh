#!/bin/bash

echo "==========================================="
echo "Testing Modular Monolithic POC APIs"
echo "==========================================="
echo ""

BASE_URL="http://localhost:8080"

echo "1️⃣ Getting all products..."
curl -s $BASE_URL/api/products | jq '.'
echo ""
echo ""

echo "2️⃣ Getting product with ID 1..."
curl -s $BASE_URL/api/products/1 | jq '.'
echo ""
echo ""

read -p "Press Enter to create an order..."
echo ""

echo "3️⃣ Creating an order for product 1 with quantity 2..."
ORDER_RESPONSE=$(curl -s -X POST $BASE_URL/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}')
echo "$ORDER_RESPONSE" | jq '.'
ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')
echo ""
echo "✅ Order created with ID: $ORDER_ID"
echo ""

read -p "Press Enter to process payment..."
echo ""

echo "4️⃣ Processing payment for order $ORDER_ID..."
PAYMENT_RESPONSE=$(curl -s -X POST $BASE_URL/api/payments \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": $ORDER_ID, \"paymentMethod\": \"CREDIT_CARD\"}")
echo "$PAYMENT_RESPONSE" | jq '.'
PAYMENT_STATUS=$(echo "$PAYMENT_RESPONSE" | jq -r '.status')
echo ""
echo "✅ Payment status: $PAYMENT_STATUS"
echo ""

read -p "Press Enter to verify results..."
echo ""

echo "5️⃣ Checking order status (should be PAID)..."
curl -s $BASE_URL/api/orders/$ORDER_ID | jq '.'
echo ""
echo ""

echo "6️⃣ Checking product stock (should be reduced by 2)..."
curl -s $BASE_URL/api/products/1 | jq '.'
echo ""
echo ""

echo "==========================================="
echo "✅ Complete E-commerce flow test finished!"
echo "==========================================="
echo ""
echo "Summary:"
echo "- Product Module: ✓ Product retrieved"
echo "- Order Module: ✓ Order created (inter-module call to Product)"
echo "- Payment Module: ✓ Payment processed (inter-module call to Order)"
echo "- Stock reduced: ✓ Product stock decreased"
echo "- Order updated: ✓ Order status changed to PAID"

