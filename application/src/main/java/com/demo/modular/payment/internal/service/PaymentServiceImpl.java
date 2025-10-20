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
import com.demo.modular.payment.internal.domain.vo.Money;
import com.demo.modular.payment.internal.domain.vo.TransactionId;
import com.demo.modular.payment.internal.domain.vo.OrderId;
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

/**
 * Application Service for Payment module.
 * Orchestrates use cases - delegates business logic to domain layer.
 */
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
            
            // Use static factory method to create payment aggregate with validation
            Payment payment = Payment.create(
                OrderId.of(orderId),
                Money.of(order.getTotalAmount()),
                paymentMethod
            );
            
            // Process payment through payment gateway
            boolean paymentSuccess = processPaymentGateway(payment);
            
            if (paymentSuccess) {
                savedPayment = paymentRepository.save(payment);
                log.info("[Payment Module] Payment processed successfully with transaction id: {}", 
                    payment.getTransactionId());
                
                // Inter-module call: Update order status to PAID
                try {
                    orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                    log.info("[Payment Module] Order status updated to PAID");
                } catch (Exception e) {
                    log.error("[Payment Module] Failed to update order status to PAID", e);
                    // Compensation: Request refund using aggregate's business method
                    payment.requestRefund();
                    paymentRepository.save(payment);
                    log.warn("[Payment Module] [Compensation] Payment marked for refund due to order update failure");
                    throw new PaymentProcessingException(orderId, "Payment succeeded but order update failed", e);
                }
                
            } else {
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
        log.debug("[Payment Module] Fetching payment with id: {}", id);
        return paymentRepository.findById(id)
                .map(paymentMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "payment.findByOrderId", description = "Time taken to find payment by order ID")
    public Optional<PaymentDTO> getPaymentByOrderId(Long orderId) {
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
     * Process payment through payment gateway.
     * In real implementation, this would call external payment API (Stripe, PayPal, etc.)
     * 
     * @param payment the payment to process
     * @return true if payment succeeds, false otherwise
     */
    private boolean processPaymentGateway(Payment payment) {
        log.info("[Payment Module] Processing payment transaction for payment ID: {}, amount: {}", 
            payment.getId(), payment.getAmount());
        
        try {
            // Simulate payment gateway interaction
            boolean success = simulatePaymentGatewayCall(payment);
            
            if (success) {
                // Generate transaction ID
                TransactionId transactionId = TransactionId.of(UUID.randomUUID().toString());
                
                // Use aggregate's business method to mark as success
                payment.markAsSuccess(transactionId);
                log.info("[Payment Module] Payment processed successfully. Transaction ID: {}", transactionId);
            } else {
                // Use aggregate's business method to mark as failed
                payment.markAsFailed();
                log.warn("[Payment Module] Payment processing failed for payment ID: {}", payment.getId());
            }
            
            return success;
        } catch (Exception e) {
            log.error("[Payment Module] Exception during payment processing for payment ID: {}", payment.getId(), e);
            payment.markAsFailed();
            return false;
        }
    }

    /**
     * Simulate payment gateway processing.
     * In real implementation, this would call external payment API.
     * 
     * @param payment the payment to process
     * @return true if payment succeeds, false otherwise
     */
    private boolean simulatePaymentGatewayCall(Payment payment) {
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
}

