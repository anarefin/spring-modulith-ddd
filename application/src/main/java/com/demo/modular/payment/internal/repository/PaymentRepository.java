package com.demo.modular.payment.internal.repository;

import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.internal.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.jmolecules.ddd.annotation.Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByStatus(PaymentStatus status);
}

