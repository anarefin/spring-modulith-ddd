package com.demo.modular.payment.service;

import com.demo.modular.order.domain.Order;
import com.demo.modular.order.domain.OrderStatus;
import com.demo.modular.order.service.OrderService;
import com.demo.modular.payment.domain.Payment;
import com.demo.modular.payment.domain.PaymentStatus;
import com.demo.modular.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService; // Inter-module dependency

    @Override
    public Payment processPayment(Long orderId, String paymentMethod) {
        log.info("Processing payment for order {} with method {}", orderId, paymentMethod);
        
        // Inter-module call: Validate order exists and get order details
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Check if order is in valid state for payment
        if (!order.isPending()) {
            throw new RuntimeException("Order is not in pending state. Current status: " + order.getStatus());
        }
        
        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent() && existingPayment.get().isSuccess()) {
            throw new RuntimeException("Payment already processed for order: " + orderId);
        }
        
        // Create payment
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        
        // Simulate payment processing
        boolean paymentSuccess = simulatePaymentGateway(payment);
        
        if (paymentSuccess) {
            String transactionId = UUID.randomUUID().toString();
            payment.markAsSuccess(transactionId);
            
            // Inter-module call: Update order status to PAID
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);
            
            log.info("Payment processed successfully with transaction id: {}", transactionId);
        } else {
            payment.markAsFailed();
            
            // Inter-module call: Update order status to FAILED
            orderService.updateOrderStatus(orderId, OrderStatus.FAILED);
            
            log.error("Payment processing failed for order: {}", orderId);
        }
        
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long id) {
        log.debug("Fetching payment with id: {}", id);
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        log.debug("Fetching payment for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        log.debug("Fetching all payments");
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        log.debug("Fetching payments with status: {}", status);
        return paymentRepository.findByStatus(status);
    }

    /**
     * Simulate payment gateway processing
     * In real implementation, this would call external payment API
     */
    private boolean simulatePaymentGateway(Payment payment) {
        log.info("Simulating payment gateway for amount: {}", payment.getAmount());
        
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }
}

