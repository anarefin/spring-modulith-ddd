package com.demo.modular.payment.internal.service;

import com.demo.modular.payment.api.dto.PaymentDTO;
import com.demo.modular.payment.internal.domain.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Payment entity and PaymentDTO.
 * Internal to payment module.
 */
@Component
class PaymentMapper {

    /**
     * Converts Payment entity to PaymentDTO.
     */
    public PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Converts PaymentDTO to Payment entity.
     */
    public Payment toEntity(PaymentDTO dto) {
        if (dto == null) {
            return null;
        }

        Payment payment = new Payment();
        payment.setId(dto.getId());
        payment.setOrderId(dto.getOrderId());
        payment.setAmount(dto.getAmount());
        payment.setStatus(dto.getStatus());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setTransactionId(dto.getTransactionId());
        return payment;
    }

    /**
     * Converts list of Payment entities to list of PaymentDTOs.
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

