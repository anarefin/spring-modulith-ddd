package com.demo.modular.product;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Product Module in isolation.
 * Uses @ApplicationModuleTest to test only the product module.
 */
@ApplicationModuleTest
@Transactional
class ProductModuleTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        // Given
        ProductDTO productDTO = ProductDTO.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stock(10)
                .build();

        // When
        ProductDTO created = productService.createProduct(productDTO);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Product");
        assertThat(created.getStock()).isEqualTo(10);
    }

    @Test
    void shouldGetProductById() {
        // Given
        ProductDTO productDTO = createTestProduct();

        // When
        ProductDTO found = productService.getProductById(productDTO.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(productDTO.getId());
        assertThat(found.getName()).isEqualTo(productDTO.getName());
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() {
        // When
        var result = productService.getProductById(99999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        createTestProduct();
        createTestProduct();

        // When
        List<ProductDTO> products = productService.getAllProducts();

        // Then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldGetAvailableProducts() {
        // Given
        ProductDTO inStock = createTestProduct();
        ProductDTO outOfStock = ProductDTO.builder()
                .name("Out of Stock Product")
                .description("No stock")
                .price(new BigDecimal("50.00"))
                .stock(0)
                .build();
        productService.createProduct(outOfStock);

        // When
        List<ProductDTO> available = productService.getAvailableProducts();

        // Then
        assertThat(available).isNotEmpty();
        assertThat(available).extracting(ProductDTO::getStock)
                .allMatch(stock -> stock > 0);
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        ProductDTO product = createTestProduct();
        product.setPrice(new BigDecimal("199.99"));
        product.setStock(20);

        // When
        ProductDTO updated = productService.updateProduct(product.getId(), product);

        // Then
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(updated.getStock()).isEqualTo(20);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // Given
        ProductDTO productDTO = ProductDTO.builder()
                .name("Test")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(99999L, productDTO))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        ProductDTO product = createTestProduct();

        // When
        productService.deleteProduct(product.getId());

        // Then
        assertThat(productService.getProductById(product.getId())).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(99999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldReduceStock() {
        // Given
        ProductDTO product = createTestProduct();
        Long productId = product.getId();

        // When
        productService.reduceStock(productId, 5);

        // Then
        ProductDTO updated = productService.getProductById(productId).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(5); // Started with 10, reduced by 5
    }

    @Test
    void shouldThrowExceptionWhenReducingStockBeyondAvailable() {
        // Given
        ProductDTO product = createTestProduct();

        // When & Then
        assertThatThrownBy(() -> productService.reduceStock(product.getId(), 100))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldRestoreStock() {
        // Given
        ProductDTO product = createTestProduct();
        productService.reduceStock(product.getId(), 5);

        // When
        productService.restoreStock(product.getId(), 5);

        // Then
        ProductDTO restored = productService.getProductById(product.getId()).orElseThrow();
        assertThat(restored.getStock()).isEqualTo(10); // Back to original
    }

    @Test
    void shouldCheckStockAvailability() {
        // Given
        ProductDTO product = createTestProduct();

        // When
        StockCheckDTO available = productService.checkStockAvailability(product.getId(), 5);
        StockCheckDTO unavailable = productService.checkStockAvailability(product.getId(), 100);

        // Then
        assertThat(available.isAvailable()).isTrue();
        assertThat(available.getAvailableStock()).isEqualTo(10);
        assertThat(unavailable.isAvailable()).isFalse();
    }

    @Test
    void shouldValidateNullProductDTO() {
        // When & Then
        assertThatThrownBy(() -> productService.createProduct(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldValidateNegativeStock() {
        // Given
        ProductDTO productDTO = ProductDTO.builder()
                .name("Test")
                .price(new BigDecimal("100"))
                .stock(-5)
                .build();

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(productDTO))
                .hasMessageContaining("stock");
    }

    private ProductDTO createTestProduct() {
        ProductDTO productDTO = ProductDTO.builder()
                .name("Test Product " + System.currentTimeMillis())
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stock(10)
                .build();
        return productService.createProduct(productDTO);
    }
}

