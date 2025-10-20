package com.demo.modular.payment.internal.domain;

import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.internal.domain.vo.Money;
import com.demo.modular.payment.internal.domain.vo.OrderId;
import com.demo.modular.payment.internal.domain.vo.TransactionId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payment Aggregate Root.
 * Encapsulates payment business logic and state machine.
 */
@Entity
@Table(name = "payments", schema = "payment_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId; // Stored as primitive for DB reference

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 10, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
    })
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "transaction_id"))
    })
    private TransactionId transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new payments (private - use static factory method).
     */
    private Payment(OrderId orderId, Money amount, String paymentMethod, PaymentStatus status) {
        this.orderId = orderId.getValue();
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Static Factory Method =====

    /**
     * Create a new Payment with validation.
     * This is the primary way to create new Payment aggregates.
     */
    public static Payment create(OrderId orderId, Money amount, String paymentMethod) {
        validateCreationParams(orderId, amount, paymentMethod);
        return new Payment(orderId, amount, paymentMethod, PaymentStatus.PENDING);
    }

    private static void validateCreationParams(OrderId orderId, Money amount, String paymentMethod) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Business Methods - State Machine =====

    /**
     * Mark payment as successful.
     * Enforces state transition rules and sets transaction ID.
     */
    public void markAsSuccess(TransactionId transactionId) {
        if (status == PaymentStatus.SUCCESS) {
            // Already successful, idempotent operation
            return;
        }
        
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot mark payment as SUCCESS. Payment is in %s state", status)
            );
        }
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required for successful payment");
        }
        
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark payment as failed.
     * Enforces state transition rules.
     */
    public void markAsFailed() {
        if (status == PaymentStatus.FAILED) {
            // Already failed, idempotent operation
            return;
        }
        
        if (status == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Cannot mark successful payment as FAILED");
        }
        
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Request refund for successful payment.
     */
    public void requestRefund() {
        if (status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments");
        }
        
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Query Methods =====

    /**
     * Check if payment is successful.
     */
    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }

    /**
     * Check if payment is pending.
     */
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    /**
     * Check if payment is failed.
     */
    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    /**
     * Check if payment requires refund.
     */
    public boolean requiresRefund() {
        return this.status == PaymentStatus.REFUNDED;
    }

    /**
     * Check if payment can be processed.
     */
    public boolean canBeProcessed() {
        return status == PaymentStatus.PENDING;
    }

    /**
     * Get OrderId as value object.
     */
    public OrderId getOrderIdVO() {
        return OrderId.of(this.orderId);
    }
}

