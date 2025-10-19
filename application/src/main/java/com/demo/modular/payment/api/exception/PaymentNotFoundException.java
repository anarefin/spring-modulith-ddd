package com.demo.modular.payment.api.exception;

/**
 * Exception thrown when a payment is not found.
 * This exception is part of the public API and can be caught by other modules.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final Long paymentId;

    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentNotFoundException(Long paymentId, String message) {
        super(message);
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}

