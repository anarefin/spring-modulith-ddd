package com.demo.modular.payment.internal.service;

import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.api.exception.OrderInvalidStateException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import com.demo.modular.order.service.OrderService;
import com.demo.modular.payment.api.dto.PaymentDTO;
import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.api.exception.DuplicatePaymentException;
import com.demo.modular.payment.api.exception.PaymentProcessingException;
import com.demo.modular.payment.internal.domain.Payment;
import com.demo.modular.payment.internal.repository.PaymentRepository;
import com.demo.modular.payment.service.PaymentService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional
class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService; // Inter-module dependency
    private final PaymentMapper paymentMapper;

    @Override
    @Timed(value = "payment.process", description = "Time taken to process a payment")
    public PaymentDTO processPayment(Long orderId, String paymentMethod) {
        validateOrderId(orderId);
        validatePaymentMethod(paymentMethod);
        
        log.info("[Payment Module] Processing payment for order {} with method {}", orderId, paymentMethod);
        
        Payment savedPayment = null;
        
        try {
            // Inter-module call: Validate order exists and get order details
            OrderDTO order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            log.debug("[Payment Module] Order found: {}, amount: {}", order.getId(), order.getTotalAmount());
            
            // Check if order is in valid state for payment
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new OrderInvalidStateException(orderId, order.getStatus(), 
                        "Order is not in pending state for payment");
            }
            
            // Check if payment already exists for this order (idempotency)
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
            if (existingPayment.isPresent() && existingPayment.get().isSuccess()) {
                throw new DuplicatePaymentException(orderId);
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
                
                savedPayment = paymentRepository.save(payment);
                log.info("[Payment Module] Payment processed successfully with transaction id: {}", transactionId);
                
                // Inter-module call: Update order status to PAID
                try {
                    orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                    log.info("[Payment Module] Order status updated to PAID");
                } catch (Exception e) {
                    log.error("[Payment Module] Failed to update order status to PAID", e);
                    // Compensation: Mark payment for refund
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                    log.warn("[Payment Module] [Compensation] Payment marked for refund due to order update failure");
                    throw new PaymentProcessingException(orderId, "Payment succeeded but order update failed", e);
                }
                
            } else {
                payment.markAsFailed();
                savedPayment = paymentRepository.save(payment);
                
                log.error("[Payment Module] Payment processing failed for order: {}", orderId);
                
                // Inter-module call: Update order status to FAILED
                try {
                    orderService.updateOrderStatus(orderId, OrderStatus.FAILED);
                    log.info("[Payment Module] Order status updated to FAILED");
                } catch (Exception e) {
                    log.error("[Payment Module] Failed to update order status to FAILED", e);
                    // Continue even if status update fails
                }
                
                throw new PaymentProcessingException(orderId, "Payment processing failed");
            }
            
            return paymentMapper.toDTO(savedPayment);
            
        } catch (OrderNotFoundException e) {
            log.error("[Payment Module] Order not found: {}", orderId, e);
            throw new PaymentProcessingException(orderId, "Order not found: " + orderId, e);
            
        } catch (OrderInvalidStateException e) {
            log.error("[Payment Module] Order in invalid state: {}", orderId, e);
            throw new PaymentProcessingException(orderId, "Order in invalid state: " + e.getMessage(), e);
            
        } catch (DuplicatePaymentException e) {
            log.error("[Payment Module] Duplicate payment attempt for order: {}", orderId, e);
            throw e;
            
        } catch (PaymentProcessingException e) {
            throw e;
            
        } catch (Exception e) {
            log.error("[Payment Module] Unexpected error processing payment for order: {}", orderId, e);
            throw new PaymentProcessingException(orderId, "Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "payment.findById", description = "Time taken to find payment by ID")
    public Optional<PaymentDTO> getPaymentById(Long id) {
        validateId(id);
        log.debug("[Payment Module] Fetching payment with id: {}", id);
        return paymentRepository.findById(id)
                .map(paymentMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "payment.findByOrderId", description = "Time taken to find payment by order ID")
    public Optional<PaymentDTO> getPaymentByOrderId(Long orderId) {
        validateOrderId(orderId);
        log.debug("[Payment Module] Fetching payment for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId)
                .map(paymentMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "payment.findAll", description = "Time taken to find all payments")
    public List<PaymentDTO> getAllPayments() {
        log.debug("[Payment Module] Fetching all payments");
        List<Payment> payments = paymentRepository.findAll();
        return paymentMapper.toDTOList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "payment.findByStatus", description = "Time taken to find payments by status")
    public List<PaymentDTO> getPaymentsByStatus(PaymentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
        log.debug("[Payment Module] Fetching payments with status: {}", status);
        List<Payment> payments = paymentRepository.findByStatus(status);
        return paymentMapper.toDTOList(payments);
    }

    /**
     * Simulate payment gateway processing.
     * In real implementation, this would call external payment API (Stripe, PayPal, etc.)
     * 
     * @param payment the payment to process
     * @return true if payment succeeds, false otherwise
     */
    private boolean simulatePaymentGateway(Payment payment) {
        log.info("[Payment Module] [External Call] Simulating payment gateway for amount: {}", payment.getAmount());
        
        try {
            // Simulate network delay
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Payment ID must be positive");
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
    }
}

