package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.internal.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Product entity and ProductDTO.
 * Internal to product module.
 * Handles conversion between domain value objects and primitive types.
 */
@Component
class ProductMapper {

    /**
     * Converts Product aggregate to ProductDTO.
     * Extracts primitive values from value objects.
     */
    public ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName().getValue())
                .description(product.getDescription())
                .price(product.getPrice().getAmount())
                .stock(product.getStock().getValue())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Converts list of Product aggregates to list of ProductDTOs.
     */
    public List<ProductDTO> toDTOList(List<Product> products) {
        if (products == null) {
            return List.of();
        }

        return products.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

