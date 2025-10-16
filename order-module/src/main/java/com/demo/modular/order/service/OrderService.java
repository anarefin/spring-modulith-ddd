package com.demo.modular.order.service;

import com.demo.modular.order.domain.Order;
import com.demo.modular.order.domain.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Order Service Interface - exposed for inter-module communication
 */
public interface OrderService {
    
    Order createOrder(Long productId, Integer quantity);
    
    Optional<Order> getOrderById(Long id);
    
    List<Order> getAllOrders();
    
    List<Order> getOrdersByStatus(OrderStatus status);
    
    /**
     * Update order status - called by Payment module
     */
    void updateOrderStatus(Long orderId, OrderStatus status);
    
    void cancelOrder(Long id);
}

