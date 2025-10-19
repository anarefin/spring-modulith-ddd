package com.demo.modular.product.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.api.exception.ProductValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Product Service Interface - Public API for inter-module communication.
 * 
 * <p>This interface defines the contract for product management operations.
 * All methods use DTOs to prevent domain model exposure to other modules.</p>
 * 
 * <p><b>Transaction Boundaries:</b> All methods except read-only operations 
 * run in their own transaction context.</p>
 * 
 * <p><b>Thread Safety:</b> All implementations must be thread-safe.</p>
 */
public interface ProductService {
    
    /**
     * Creates a new product in the system.
     * 
     * @param productDTO the product data to create
     * @return the created product with generated ID
     * @throws ProductValidationException if validation fails
     * @throws IllegalArgumentException if productDTO is null
     */
    ProductDTO createProduct(@Valid @NotNull ProductDTO productDTO);
    
    /**
     * Retrieves a product by its ID.
     * 
     * @param id the product ID
     * @return Optional containing the product if found, empty otherwise
     * @throws IllegalArgumentException if id is null or negative
     */
    Optional<ProductDTO> getProductById(@NotNull Long id);
    
    /**
     * Retrieves all products in the system.
     * 
     * @return list of all products, empty list if none exist
     */
    List<ProductDTO> getAllProducts();
    
    /**
     * Retrieves all products with stock greater than zero.
     * 
     * @return list of available products, empty list if none exist
     */
    List<ProductDTO> getAvailableProducts();
    
    /**
     * Updates an existing product.
     * 
     * @param id the product ID to update
     * @param productDTO the updated product data
     * @return the updated product
     * @throws ProductNotFoundException if product with given ID doesn't exist
     * @throws ProductValidationException if validation fails
     * @throws IllegalArgumentException if parameters are null
     */
    ProductDTO updateProduct(@NotNull Long id, @Valid @NotNull ProductDTO productDTO);
    
    /**
     * Deletes a product by its ID.
     * 
     * @param id the product ID to delete
     * @throws ProductNotFoundException if product doesn't exist
     * @throws IllegalArgumentException if id is null
     */
    void deleteProduct(@NotNull Long id);
    
    /**
     * Reduces product stock by the specified quantity.
     * 
     * <p><b>Inter-Module Usage:</b> Called by Order module during order creation.</p>
     * 
     * <p><b>Transaction:</b> Runs in REQUIRES_NEW propagation to ensure 
     * independent transaction boundary.</p>
     * 
     * <p><b>Idempotency:</b> This operation is NOT idempotent. Multiple calls 
     * with same parameters will reduce stock multiple times.</p>
     * 
     * @param productId the ID of the product
     * @param quantity the quantity to reduce
     * @throws ProductNotFoundException if product doesn't exist
     * @throws InsufficientStockException if available stock is less than requested quantity
     * @throws IllegalArgumentException if productId is null or quantity is null/negative
     */
    void reduceStock(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
    
    /**
     * Restores product stock by the specified quantity.
     * 
     * <p><b>Compensation Method:</b> Used for rollback when order creation 
     * or payment fails after stock has been reduced.</p>
     * 
     * <p><b>Inter-Module Usage:</b> Called by Order module for compensation.</p>
     * 
     * @param productId the ID of the product
     * @param quantity the quantity to restore
     * @throws ProductNotFoundException if product doesn't exist
     * @throws IllegalArgumentException if parameters are null or quantity is negative
     */
    void restoreStock(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
    
    /**
     * Checks if product has sufficient stock for the requested quantity.
     * 
     * <p><b>Inter-Module Usage:</b> Called by Order module before order creation.</p>
     * 
     * <p><b>Note:</b> This check is not transactionally protected with stock reduction.
     * Use optimistic locking or database constraints to handle concurrent stock modifications.</p>
     * 
     * @param productId the ID of the product
     * @param quantity the quantity to check
     * @return StockCheckDTO containing availability status and details
     * @throws ProductNotFoundException if product doesn't exist
     * @throws IllegalArgumentException if parameters are null or quantity is negative
     */
    StockCheckDTO checkStockAvailability(@NotNull Long productId, @NotNull @Min(1) Integer quantity);
}

