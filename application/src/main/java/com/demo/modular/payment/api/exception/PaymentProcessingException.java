package com.demo.modular.payment.api.exception;

/**
 * Exception thrown when payment processing fails.
 * This exception is part of the public API and can be caught by other modules.
 */
public class PaymentProcessingException extends RuntimeException {

    private final Long orderId;

    public PaymentProcessingException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public PaymentProcessingException(Long orderId, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}

