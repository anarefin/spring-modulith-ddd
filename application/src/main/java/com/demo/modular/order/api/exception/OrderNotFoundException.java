package com.demo.modular.order.api.exception;

/**
 * Exception thrown when an order is not found.
 * This exception is part of the public API and can be caught by other modules.
 */
public class OrderNotFoundException extends RuntimeException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public OrderNotFoundException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}

