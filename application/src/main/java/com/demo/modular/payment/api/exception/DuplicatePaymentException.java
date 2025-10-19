package com.demo.modular.payment.api.exception;

/**
 * Exception thrown when attempting to process payment for an order that already has a successful payment.
 * This exception is part of the public API and can be caught by other modules.
 */
public class DuplicatePaymentException extends RuntimeException {

    private final Long orderId;

    public DuplicatePaymentException(Long orderId) {
        super("Payment already processed for order: " + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}

