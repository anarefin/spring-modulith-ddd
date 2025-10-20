package com.demo.modular.order.internal.domain;

import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.internal.domain.vo.Money;
import com.demo.modular.order.internal.domain.vo.ProductId;
import com.demo.modular.order.internal.domain.vo.ProductName;
import com.demo.modular.order.internal.domain.vo.Quantity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Order Aggregate Root.
 * Encapsulates order business logic and state machine.
 */
@Entity
@Table(name = "orders", schema = "order_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId; // Stored as primitive for DB reference

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "product_name"))
    })
    private ProductName productName;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    })
    private Quantity quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
    })
    private Money totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new orders (private - use static factory method).
     */
    private Order(ProductId productId, ProductName productName, Quantity quantity, Money totalAmount, OrderStatus status) {
        this.productId = productId.getValue();
        this.productName = productName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Static Factory Method =====

    /**
     * Create a new Order with validation.
     * This is the primary way to create new Order aggregates.
     */
    public static Order create(ProductId productId, ProductName productName, Quantity quantity, Money totalAmount) {
        validateCreationParams(productId, productName, quantity, totalAmount);
        return new Order(productId, productName, quantity, totalAmount, OrderStatus.PENDING);
    }

    private static void validateCreationParams(ProductId productId, ProductName productName, Quantity quantity, Money totalAmount) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (productName == null) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (totalAmount == null || !totalAmount.isPositive()) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Business Methods - State Machine =====

    /**
     * Mark order as paid.
     * Enforces state transition rules.
     */
    public void markAsPaid() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot mark order as PAID. Order is in %s state", status)
            );
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark order as failed.
     * Enforces state transition rules.
     */
    public void markAsFailed() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot mark order as FAILED. Order is in %s state", status)
            );
        }
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancel the order.
     * Enforces state transition rules.
     */
    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid order");
        }
        if (status == OrderStatus.CANCELLED) {
            // Already cancelled, idempotent operation
            return;
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Query Methods =====

    /**
     * Check if order is pending.
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * Check if order is paid.
     */
    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    /**
     * Check if order is cancelled.
     */
    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    /**
     * Check if order can be cancelled.
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.FAILED;
    }

    /**
     * Check if order can process payment.
     */
    public boolean canProcessPayment() {
        return status == OrderStatus.PENDING;
    }

    /**
     * Get ProductId as value object.
     */
    public ProductId getProductIdVO() {
        return ProductId.of(this.productId);
    }
}

