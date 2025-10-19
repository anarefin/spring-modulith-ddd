package com.demo.modular.payment.service;

import com.demo.modular.payment.api.dto.PaymentDTO;
import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.api.exception.DuplicatePaymentException;
import com.demo.modular.payment.api.exception.PaymentProcessingException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Payment Service Interface - Public API for inter-module communication.
 * 
 * <p>This interface defines the contract for payment processing operations.
 * All methods use DTOs to prevent domain model exposure to other modules.</p>
 * 
 * <p><b>Transaction Boundaries:</b> Payment processing involves cross-module calls
 * to Order module. Each service manages its own transaction.</p>
 * 
 * <p><b>Thread Safety:</b> All implementations must be thread-safe.</p>
 * 
 * <p><b>Idempotency:</b> Payment processing checks for duplicate payments to
 * prevent double-charging.</p>
 */
public interface PaymentService {
    
    /**
     * Processes payment for an order.
     * 
     * <p><b>Inter-Module Dependencies:</b></p>
     * <ul>
     *   <li>Calls OrderService.getOrderById() to validate order exists and is payable</li>
     *   <li>Calls OrderService.updateOrderStatus() to update order status based on payment result</li>
     * </ul>
     * 
     * <p><b>Transaction:</b> Payment creation and order status update run in separate
     * transactions (REQUIRES_NEW propagation) to ensure proper isolation.</p>
     * 
     * <p><b>Compensation:</b> If payment succeeds but order status update fails,
     * the payment will be marked for refund.</p>
     * 
     * @param orderId the order ID to process payment for
     * @param paymentMethod the payment method (e.g., "CREDIT_CARD", "DEBIT_CARD", "PAYPAL")
     * @return the processed payment with status SUCCESS or FAILED
     * @throws PaymentProcessingException if payment processing fails
     * @throws DuplicatePaymentException if payment already exists for the order
     * @throws IllegalArgumentException if parameters are null or invalid
     */
    PaymentDTO processPayment(@NotNull Long orderId, @NotBlank String paymentMethod);
    
    /**
     * Retrieves a payment by its ID.
     * 
     * @param id the payment ID
     * @return Optional containing the payment if found, empty otherwise
     * @throws IllegalArgumentException if id is null or negative
     */
    Optional<PaymentDTO> getPaymentById(@NotNull Long id);
    
    /**
     * Retrieves a payment by order ID.
     * 
     * @param orderId the order ID
     * @return Optional containing the payment if found, empty otherwise
     * @throws IllegalArgumentException if orderId is null or negative
     */
    Optional<PaymentDTO> getPaymentByOrderId(@NotNull Long orderId);
    
    /**
     * Retrieves all payments in the system.
     * 
     * @return list of all payments, empty list if none exist
     */
    List<PaymentDTO> getAllPayments();
    
    /**
     * Retrieves payments by status.
     * 
     * @param status the payment status to filter by
     * @return list of payments with the given status, empty list if none exist
     * @throws IllegalArgumentException if status is null
     */
    List<PaymentDTO> getPaymentsByStatus(@NotNull PaymentStatus status);
}

