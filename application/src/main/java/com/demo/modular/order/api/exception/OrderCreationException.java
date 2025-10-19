package com.demo.modular.order.api.exception;

/**
 * Exception thrown when order creation fails.
 * This exception is part of the public API and can be caught by other modules.
 */
public class OrderCreationException extends RuntimeException {

    public OrderCreationException(String message) {
        super(message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

