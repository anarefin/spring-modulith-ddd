package com.demo.modular.order.internal.service;

import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.internal.domain.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Order aggregate and OrderDTO.
 * Internal to order module.
 * Handles conversion between domain value objects and primitive types.
 */
@Component
class OrderMapper {

    /**
     * Converts Order aggregate to OrderDTO.
     * Extracts primitive values from value objects.
     */
    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }

        return OrderDTO.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productName(order.getProductName().getValue())
                .quantity(order.getQuantity().getValue())
                .totalAmount(order.getTotalAmount().getAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Converts list of Order aggregates to list of OrderDTOs.
     */
    public List<OrderDTO> toDTOList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }

        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

