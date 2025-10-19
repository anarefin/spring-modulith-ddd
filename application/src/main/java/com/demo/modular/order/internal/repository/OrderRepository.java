package com.demo.modular.order.internal.repository;

import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.internal.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByProductId(Long productId);
}

