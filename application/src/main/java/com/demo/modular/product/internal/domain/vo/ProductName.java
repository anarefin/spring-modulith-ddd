package com.demo.modular.product.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing Product Name.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
@ValueObject
public class ProductName {
    
    private String value;
    
    private ProductName(String value) {
        validate(value);
        this.value = value.trim();
    }
    
    public static ProductName of(String value) {
        return new ProductName(value);
    }
    
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (value.trim().length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 characters");
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}

