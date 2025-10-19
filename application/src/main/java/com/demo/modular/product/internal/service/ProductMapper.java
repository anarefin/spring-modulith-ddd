package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.internal.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Product entity and ProductDTO.
 * Internal to product module.
 */
@Component
class ProductMapper {

    /**
     * Converts Product entity to ProductDTO.
     */
    public ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Converts ProductDTO to Product entity.
     */
    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }

        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        return product;
    }

    /**
     * Updates existing Product entity with data from ProductDTO.
     */
    public void updateEntity(Product product, ProductDTO dto) {
        if (product == null || dto == null) {
            return;
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
    }

    /**
     * Converts list of Product entities to list of ProductDTOs.
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

