package com.demo.modular.payment.service;

import com.demo.modular.payment.domain.Payment;
import com.demo.modular.payment.domain.PaymentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Payment Service Interface - exposed for inter-module communication
 */
public interface PaymentService {
    
    Payment processPayment(Long orderId, String paymentMethod);
    
    Optional<Payment> getPaymentById(Long id);
    
    Optional<Payment> getPaymentByOrderId(Long orderId);
    
    List<Payment> getAllPayments();
    
    List<Payment> getPaymentsByStatus(PaymentStatus status);
}

