package com.demo.modular.order.internal.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jmolecules.ddd.annotation.ValueObject;

import java.io.Serializable;

/**
 * Value Object representing Product ID.
 * Immutable and self-validating.
 */
@Getter
@EqualsAndHashCode
@ValueObject
public class ProductId implements Serializable {
    
    private final Long value;
    
    private ProductId(Long value) {
        validate(value);
        this.value = value;
    }
    
    public static ProductId of(Long value) {
        return new ProductId(value);
    }
    
    private void validate(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("ProductId must be positive: " + value);
        }
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

