package com.demo.modular.product.api.exception;

/**
 * Exception thrown when a product is not found.
 * This exception is part of the public API and can be caught by other modules.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
        this.productId = productId;
    }

    public ProductNotFoundException(Long productId, String message) {
        super(message);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}

