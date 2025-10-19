package com.demo.modular.order.api.exception;

import com.demo.modular.order.api.dto.OrderStatus;

/**
 * Exception thrown when an order is in an invalid state for the requested operation.
 * This exception is part of the public API and can be caught by other modules.
 */
public class OrderInvalidStateException extends RuntimeException {

    private final Long orderId;
    private final OrderStatus currentStatus;

    public OrderInvalidStateException(Long orderId, OrderStatus currentStatus, String message) {
        super(String.format("Order %d is in invalid state %s: %s", orderId, currentStatus, message));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}

