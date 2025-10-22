package com.demo.modular.payment.internal.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing Transaction ID.
 * Immutable and self-validating.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
@ValueObject
public class TransactionId {
    
    private String value;
    
    private TransactionId(String value) {
        validate(value);
        this.value = value;
    }
    
    public static TransactionId of(String value) {
        return new TransactionId(value);
    }
    
    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TransactionId cannot be null or empty");
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}

