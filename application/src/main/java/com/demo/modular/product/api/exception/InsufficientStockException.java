package com.demo.modular.product.api.exception;

/**
 * Exception thrown when attempting to reduce stock beyond available quantity.
 * This exception is part of the public API and can be caught by other modules.
 */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final Integer requested;
    private final Integer available;

    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product %d. Requested: %d, Available: %d", 
            productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}

