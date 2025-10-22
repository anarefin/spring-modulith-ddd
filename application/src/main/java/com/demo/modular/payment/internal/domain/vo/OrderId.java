package com.demo.modular.payment.internal.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jmolecules.ddd.annotation.ValueObject;

import java.io.Serializable;

/**
 * Value Object representing Order ID.
 * Immutable and self-validating.
 */
@Getter
@EqualsAndHashCode
@ValueObject
public class OrderId implements Serializable {
    
    private final Long value;
    
    private OrderId(Long value) {
        validate(value);
        this.value = value;
    }
    
    public static OrderId of(Long value) {
        return new OrderId(value);
    }
    
    private void validate(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderId cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("OrderId must be positive: " + value);
        }
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

