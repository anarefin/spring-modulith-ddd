package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.api.exception.ProductValidationException;
import com.demo.modular.product.internal.domain.Product;
import com.demo.modular.product.internal.domain.vo.Money;
import com.demo.modular.product.internal.domain.vo.ProductName;
import com.demo.modular.product.internal.domain.vo.Quantity;
import com.demo.modular.product.internal.repository.ProductRepository;
import com.demo.modular.product.service.ProductService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

/**
 * Application Service for Product module.
 * Orchestrates use cases - delegates business logic to domain layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional
class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Timed(value = "product.create", description = "Time taken to create a product")
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("[Product Module] Creating product: {}", productDTO.getName());
        
        try {
            // Use static factory method to create aggregate with validation
            Product product = Product.create(
                ProductName.of(productDTO.getName()),
                productDTO.getDescription(),
                Money.of(productDTO.getPrice()),
                Quantity.of(productDTO.getStock())
            );
            
            Product savedProduct = productRepository.save(product);
            log.info("[Product Module] Product created successfully with id: {}", savedProduct.getId());
            return productMapper.toDTO(savedProduct);
        } catch (Exception e) {
            log.error("[Product Module] Failed to create product: {}", productDTO.getName(), e);
            throw new ProductValidationException("Failed to create product: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "product.findById", description = "Time taken to find product by ID")
    public Optional<ProductDTO> getProductById(Long id) {
        log.debug("[Product Module] Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "product.findAll", description = "Time taken to find all products")
    public List<ProductDTO> getAllProducts() {
        log.debug("[Product Module] Fetching all products");
        List<Product> products = productRepository.findAll();
        return productMapper.toDTOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "product.findAvailable", description = "Time taken to find available products")
    public List<ProductDTO> getAvailableProducts() {
        log.debug("[Product Module] Fetching available products");
        // Query all products and filter using aggregate's business method
        List<Product> products = productRepository.findByStockGreaterThan(0);
        return productMapper.toDTOList(products);
    }

    @Override
    @Timed(value = "product.update", description = "Time taken to update a product")
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("[Product Module] Updating product with id: {}", id);
        
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        try {
            // Use aggregate's update method - all business logic in domain
            existingProduct.update(
                ProductName.of(productDTO.getName()),
                productDTO.getDescription(),
                Money.of(productDTO.getPrice()),
                Quantity.of(productDTO.getStock())
            );
            
            // Service layer only persists the updated aggregate
            Product savedProduct = productRepository.save(existingProduct);
            log.info("[Product Module] Product updated successfully: {}", id);
            return productMapper.toDTO(savedProduct);
        } catch (IllegalArgumentException e) {
            log.error("[Product Module] Failed to update product: {}", id, e);
            throw new ProductValidationException("Failed to update product: " + e.getMessage(), e);
        }
    }

    @Override
    @Timed(value = "product.delete", description = "Time taken to delete a product")
    public void deleteProduct(Long id) {
        log.info("[Product Module] Deleting product with id: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        try {
            // Use aggregate's business method to validate deletion is allowed
            product.validateDeletion();
            
            // Service layer only performs the actual deletion after validation
            productRepository.deleteById(id);
            log.info("[Product Module] Product deleted successfully: {}", id);
        } catch (IllegalStateException e) {
            log.error("[Product Module] Cannot delete product: {}", id, e);
            throw new ProductValidationException("Cannot delete product: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "product.reduceStock", description = "Time taken to reduce stock")
    public void reduceStock(Long productId, Integer quantity) {
        log.info("[Product Module] [Inter-Module Call] Reducing stock for product {} by {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        Quantity requestedQuantity = Quantity.of(quantity);
        
        try {
            // Check stock availability and reserve using aggregate methods
            if (!product.hasAvailableStock(requestedQuantity)) {
                log.error("[Product Module] Insufficient stock for product {}. Available: {}, Requested: {}", 
                    productId, product.getStock(), requestedQuantity);
                throw new InsufficientStockException(productId, quantity, product.getStock().getValue());
            }
            
            product.reserveStock(requestedQuantity);
            productRepository.save(product);
            log.info("[Product Module] Stock reduced successfully for product {}. New stock: {}", 
                    productId, product.getStock());
        } catch (IllegalStateException e) {
            log.error("[Product Module] Failed to reduce stock for product {}", productId, e);
            throw new InsufficientStockException(productId, quantity, product.getStock().getValue());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "product.restoreStock", description = "Time taken to restore stock")
    public void restoreStock(Long productId, Integer quantity) {
        log.info("[Product Module] [Compensation] Restoring stock for product {} by {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        Quantity restoredQuantity = Quantity.of(quantity);
        
        // Use aggregate method for stock restoration
        product.releaseStock(restoredQuantity);
        productRepository.save(product);
        log.info("[Product Module] [Compensation] Stock restored successfully for product {}. New stock: {}", 
                productId, product.getStock());
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "product.checkStock", description = "Time taken to check stock availability")
    public StockCheckDTO checkStockAvailability(Long productId, Integer quantity) {
        log.debug("[Product Module] [Inter-Module Call] Checking stock availability for product {} with quantity {}", 
                productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        Quantity requestedQuantity = Quantity.of(quantity);
        
        // Delegate to aggregate method
        boolean isAvailable = product.hasAvailableStock(requestedQuantity);
        
        return StockCheckDTO.builder()
                .productId(productId)
                .requestedQuantity(quantity)
                .availableStock(product.getStock().getValue())
                .isAvailable(isAvailable)
                .build();
    }
}

