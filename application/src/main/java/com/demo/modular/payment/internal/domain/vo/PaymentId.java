package com.demo.modular.payment.internal.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jmolecules.ddd.annotation.ValueObject;

import java.io.Serializable;

/**
 * Value Object representing Payment ID.
 * Immutable and self-validating.
 */
@Getter
@EqualsAndHashCode
@ValueObject
public class PaymentId implements Serializable {
    
    private final Long value;
    
    private PaymentId(Long value) {
        validate(value);
        this.value = value;
    }
    
    public static PaymentId of(Long value) {
        return new PaymentId(value);
    }
    
    private void validate(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("PaymentId must be positive: " + value);
        }
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

