package com.demo.modular.order.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing quantity.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
@ValueObject
public class Quantity {
    
    private Integer value;
    
    private Quantity(Integer value) {
        validate(value);
        this.value = value;
    }
    
    public static Quantity of(Integer value) {
        return new Quantity(value);
    }
    
    public static Quantity zero() {
        return new Quantity(0);
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }
    
    public Quantity subtract(Quantity other) {
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Cannot subtract quantity resulting in negative value");
        }
        return new Quantity(result);
    }
    
    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }
    
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= other.value;
    }
    
    public boolean isLessThan(Quantity other) {
        return this.value < other.value;
    }
    
    public boolean isPositive() {
        return value > 0;
    }
    
    public boolean isZero() {
        return value == 0;
    }
    
    private void validate(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Quantity value cannot be null");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

