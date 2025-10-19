package com.demo.modular.payment.internal.repository;

import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.internal.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByStatus(PaymentStatus status);
}

