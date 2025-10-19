package com.demo.modular.payment.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Payment entity.
 * Used for cross-module communication to avoid exposing domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Long id;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private PaymentStatus status;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String transactionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

