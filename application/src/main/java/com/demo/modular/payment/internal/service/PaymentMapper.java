package com.demo.modular.payment.internal.service;

import com.demo.modular.payment.api.dto.PaymentDTO;
import com.demo.modular.payment.internal.domain.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Payment aggregate and PaymentDTO.
 * Internal to payment module.
 * Handles conversion between domain value objects and primitive types.
 */
@Component
class PaymentMapper {

    /**
     * Converts Payment aggregate to PaymentDTO.
     * Extracts primitive values from value objects.
     */
    public PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount().getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId() != null ? payment.getTransactionId().getValue() : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Converts list of Payment aggregates to list of PaymentDTOs.
     */
    public List<PaymentDTO> toDTOList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }

        return payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

