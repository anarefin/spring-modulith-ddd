package com.demo.modular.product.api.exception;

/**
 * Exception thrown when product validation fails.
 * This exception is part of the public API and can be caught by other modules.
 */
public class ProductValidationException extends RuntimeException {

    public ProductValidationException(String message) {
        super(message);
    }

    public ProductValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

