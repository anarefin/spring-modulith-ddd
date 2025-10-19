package com.demo.modular.order.internal.service;

import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.internal.domain.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Order entity and OrderDTO.
 * Internal to order module.
 */
@Component
class OrderMapper {

    /**
     * Converts Order entity to OrderDTO.
     */
    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }

        return OrderDTO.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Converts OrderDTO to Order entity.
     */
    public Order toEntity(OrderDTO dto) {
        if (dto == null) {
            return null;
        }

        Order order = new Order();
        order.setId(dto.getId());
        order.setProductId(dto.getProductId());
        order.setProductName(dto.getProductName());
        order.setQuantity(dto.getQuantity());
        order.setTotalAmount(dto.getTotalAmount());
        order.setStatus(dto.getStatus());
        return order;
    }

    /**
     * Converts list of Order entities to list of OrderDTOs.
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

