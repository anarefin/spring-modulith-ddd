package com.demo.modular.product.service;

import com.demo.modular.product.domain.Product;

import java.util.List;
import java.util.Optional;

/**
 * Product Service Interface - exposed for inter-module communication
 */
public interface ProductService {
    
    Product createProduct(Product product);
    
    Optional<Product> getProductById(Long id);
    
    List<Product> getAllProducts();
    
    List<Product> getAvailableProducts();
    
    Product updateProduct(Long id, Product product);
    
    void deleteProduct(Long id);
    
    /**
     * Reduce product stock - called by Order module
     */
    void reduceStock(Long productId, Integer quantity);
    
    /**
     * Check if product has sufficient stock - called by Order module
     */
    boolean hasAvailableStock(Long productId, Integer quantity);
}

