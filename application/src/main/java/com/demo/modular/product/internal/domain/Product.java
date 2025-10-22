package com.demo.modular.product.internal.domain;

import com.demo.modular.product.internal.domain.vo.Money;
import com.demo.modular.product.internal.domain.vo.ProductName;
import com.demo.modular.product.internal.domain.vo.Quantity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;

/**
 * Product Aggregate Root.
 * Encapsulates product business logic and invariants.
 */
@Entity
@Table(name = "products", schema = "product_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
@AggregateRoot
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Identity
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    })
    private ProductName name;

    @Column(length = 1000)
    private String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 10, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
    })
    private Money price;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "stock", nullable = false))
    })
    private Quantity stock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new products (private - use static factory method).
     */
    private Product(ProductName name, String description, Money price, Quantity initialStock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = initialStock;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Static Factory Method =====

    /**
     * Create a new Product with validation.
     * This is the primary way to create new Product aggregates.
     */
    public static Product create(ProductName name, String description, Money price, Quantity initialStock) {
        validateCreationParams(name, price, initialStock);
        return new Product(name, description, price, initialStock);
    }

    private static void validateCreationParams(ProductName name, Money price, Quantity stock) {
        if (name == null) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null) {
            throw new IllegalArgumentException("Product price is required");
        }
        if (!price.isPositive()) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (stock == null) {
            throw new IllegalArgumentException("Product stock is required");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Business Methods =====

    /**
     * Check if product has available stock for the requested quantity.
     */
    public boolean hasAvailableStock(Quantity requestedQuantity) {
        return stock.isGreaterThanOrEqual(requestedQuantity);
    }

    /**
     * Reserve stock for an order.
     * Enforces invariant: stock cannot be negative.
     */
    public void reserveStock(Quantity quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %s, Requested: %s", stock, quantity)
            );
        }
        this.stock = this.stock.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release stock (e.g., when order is cancelled).
     */
    public void releaseStock(Quantity quantity) {
        this.stock = this.stock.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update product price.
     * Enforces invariant: price must be positive.
     */
    public void updatePrice(Money newPrice) {
        if (!newPrice.isPositive()) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Change product name.
     */
    public void changeName(ProductName newName) {
        if (newName == null) {
            throw new IllegalArgumentException("Product name cannot be null");
        }
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update product description.
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update stock quantity directly (admin operation).
     */
    public void updateStock(Quantity newStock) {
        if (newStock == null) {
            throw new IllegalArgumentException("Stock quantity cannot be null");
        }
        this.stock = newStock;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if product is available for sale.
     */
    public boolean isAvailable() {
        return stock.isPositive();
    }

    /**
     * Update product with new values.
     * This is the primary method for updating product information.
     * Enforces all business invariants during update.
     *
     * @param newName new product name (required)
     * @param newDescription new product description (can be null)
     * @param newPrice new product price (required, must be positive)
     * @param newStock new stock quantity (required, cannot be negative)
     */
    public void update(ProductName newName, String newDescription, Money newPrice, Quantity newStock) {
        // Validate all parameters
        if (newName == null) {
            throw new IllegalArgumentException("Product name cannot be null");
        }
        if (newPrice == null) {
            throw new IllegalArgumentException("Product price cannot be null");
        }
        if (!newPrice.isPositive()) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (newStock == null) {
            throw new IllegalArgumentException("Stock quantity cannot be null");
        }
        if (newStock.getValue() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        // Apply changes - all validations passed
        this.name = newName;
        this.description = newDescription;
        this.price = newPrice;
        this.stock = newStock;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if product can be deleted.
     * Business rule: Cannot delete product if it has stock.
     * 
     * @return true if product can be deleted, false otherwise
     */
    public boolean canBeDeleted() {
        // Business rule: Cannot delete products with existing stock
        return !stock.isPositive();
    }

    /**
     * Validate deletion is allowed and throw exception if not.
     * This method should be called before actual deletion.
     * 
     * @throws IllegalStateException if product cannot be deleted
     */
    public void validateDeletion() {
        if (stock.isPositive()) {
            throw new IllegalStateException(
                String.format("Cannot delete product with existing stock. Current stock: %s", stock.getValue())
            );
        }
    }
}

