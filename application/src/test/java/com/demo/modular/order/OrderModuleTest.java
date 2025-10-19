package com.demo.modular.order;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.api.exception.OrderCreationException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.service.OrderService;
import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Order Module.
 * Tests include inter-module communication with Product module.
 */
@ApplicationModuleTest
@Transactional
class OrderModuleTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    private ProductDTO testProduct;

    @BeforeEach
    void setup() {
        // Create a test product for orders
        testProduct = ProductDTO.builder()
                .name("Test Product for Orders")
                .description("Product for order testing")
                .price(new BigDecimal("100.00"))
                .stock(50)
                .build();
        testProduct = productService.createProduct(testProduct);
    }

    @Test
    void shouldCreateOrder() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(5)
                .build();

        // When
        OrderDTO created = orderService.createOrder(request);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getProductId()).isEqualTo(testProduct.getId());
        assertThat(created.getQuantity()).isEqualTo(5);
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldReduceStockWhenCreatingOrder() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(5)
                .build();
        Integer initialStock = testProduct.getStock();

        // When
        orderService.createOrder(request);

        // Then
        ProductDTO updatedProduct = productService.getProductById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 5);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(99999L)
                .quantity(5)
                .build();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(1000) // More than available
                .build();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldGetOrderById() {
        // Given
        OrderDTO created = createTestOrder();

        // When
        OrderDTO found = orderService.getOrderById(created.getId()).orElseThrow();

        // Then
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getProductId()).isEqualTo(testProduct.getId());
    }

    @Test
    void shouldGetAllOrders() {
        // Given
        createTestOrder();
        createTestOrder();

        // When
        List<OrderDTO> orders = orderService.getAllOrders();

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldGetOrdersByStatus() {
        // Given
        createTestOrder();

        // When
        List<OrderDTO> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Then
        assertThat(pendingOrders).isNotEmpty();
        assertThat(pendingOrders).allMatch(order -> order.getStatus() == OrderStatus.PENDING);
    }

    @Test
    void shouldUpdateOrderStatus() {
        // Given
        OrderDTO order = createTestOrder();

        // When
        orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);

        // Then
        OrderDTO updated = orderService.getOrderById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentOrder() {
        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(99999L, OrderStatus.PAID))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void shouldCancelOrder() {
        // Given
        OrderDTO order = createTestOrder();

        // When
        orderService.cancelOrder(order.getId());

        // Then
        OrderDTO cancelled = orderService.getOrderById(order.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldRestoreStockWhenCancellingOrder() {
        // Given
        OrderDTO order = createTestOrder();
        Integer stockAfterOrder = productService.getProductById(testProduct.getId()).orElseThrow().getStock();

        // When
        orderService.cancelOrder(order.getId());

        // Then
        ProductDTO product = productService.getProductById(testProduct.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(stockAfterOrder + order.getQuantity());
    }

    @Test
    void shouldValidateNullOrderRequest() {
        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateInvalidQuantity() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(0)
                .build();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity");
    }

    private OrderDTO createTestOrder() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build();
        return orderService.createOrder(request);
    }
}

