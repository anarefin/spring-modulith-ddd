package com.demo.modular.order.service;

import com.demo.modular.order.domain.Order;
import com.demo.modular.order.domain.OrderStatus;
import com.demo.modular.order.repository.OrderRepository;
import com.demo.modular.product.domain.Product;
import com.demo.modular.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService; // Inter-module dependency

    @Override
    public Order createOrder(Long productId, Integer quantity) {
        log.info("Creating order for product {} with quantity {}", productId, quantity);
        
        // Inter-module call: Get product details from Product module
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        // Inter-module call: Check stock availability
        if (!productService.hasAvailableStock(productId, quantity)) {
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }
        
        // Calculate total amount
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Create order
        Order order = new Order();
        order.setProductId(productId);
        order.setProductName(product.getName()); // Store snapshot
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);
        
        // Inter-module call: Reduce product stock
        productService.reduceStock(productId, quantity);
        
        log.info("Order created successfully with id: {}", savedOrder.getId());
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    @Override
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("Updating order {} status to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order status updated successfully");
    }

    @Override
    public void cancelOrder(Long id) {
        log.info("Cancelling order with id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}

