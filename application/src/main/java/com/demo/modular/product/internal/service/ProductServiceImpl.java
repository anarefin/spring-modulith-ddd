package com.demo.modular.product.internal.service;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.product.api.exception.ProductNotFoundException;
import com.demo.modular.product.api.exception.ProductValidationException;
import com.demo.modular.product.internal.domain.Product;
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
        validateProductDTO(productDTO);
        log.info("[Product Module] Creating product: {}", productDTO.getName());
        
        try {
            Product product = productMapper.toEntity(productDTO);
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
        validateId(id);
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
        List<Product> products = productRepository.findByStockGreaterThan(0);
        return productMapper.toDTOList(products);
    }

    @Override
    @Timed(value = "product.update", description = "Time taken to update a product")
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        validateId(id);
        validateProductDTO(productDTO);
        log.info("[Product Module] Updating product with id: {}", id);
        
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        productMapper.updateEntity(existingProduct, productDTO);
        Product savedProduct = productRepository.save(existingProduct);
        log.info("[Product Module] Product updated successfully: {}", id);
        return productMapper.toDTO(savedProduct);
    }

    @Override
    @Timed(value = "product.delete", description = "Time taken to delete a product")
    public void deleteProduct(Long id) {
        validateId(id);
        log.info("[Product Module] Deleting product with id: {}", id);
        
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        
        productRepository.deleteById(id);
        log.info("[Product Module] Product deleted successfully: {}", id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "product.reduceStock", description = "Time taken to reduce stock")
    public void reduceStock(Long productId, Integer quantity) {
        validateId(productId);
        validateQuantity(quantity);
        
        log.info("[Product Module] [Inter-Module Call] Reducing stock for product {} by {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        if (!product.hasAvailableStock(quantity)) {
            throw new InsufficientStockException(productId, quantity, product.getStock());
        }
        
        try {
            product.reduceStock(quantity);
            productRepository.save(product);
            log.info("[Product Module] Stock reduced successfully for product {}. New stock: {}", 
                    productId, product.getStock());
        } catch (IllegalStateException e) {
            log.error("[Product Module] Failed to reduce stock for product {}", productId, e);
            throw new InsufficientStockException(productId, quantity, product.getStock());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "product.restoreStock", description = "Time taken to restore stock")
    public void restoreStock(Long productId, Integer quantity) {
        validateId(productId);
        validateQuantity(quantity);
        
        log.info("[Product Module] [Compensation] Restoring stock for product {} by {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
        log.info("[Product Module] [Compensation] Stock restored successfully for product {}. New stock: {}", 
                productId, product.getStock());
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "product.checkStock", description = "Time taken to check stock availability")
    public StockCheckDTO checkStockAvailability(Long productId, Integer quantity) {
        validateId(productId);
        validateQuantity(quantity);
        
        log.debug("[Product Module] [Inter-Module Call] Checking stock availability for product {} with quantity {}", 
                productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        boolean isAvailable = product.hasAvailableStock(quantity);
        
        return StockCheckDTO.builder()
                .productId(productId)
                .requestedQuantity(quantity)
                .availableStock(product.getStock())
                .isAvailable(isAvailable)
                .build();
    }

    private void validateProductDTO(ProductDTO productDTO) {
        if (productDTO == null) {
            throw new IllegalArgumentException("ProductDTO cannot be null");
        }
        if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
            throw new ProductValidationException("Product name is required");
        }
        if (productDTO.getPrice() == null || productDTO.getPrice().signum() <= 0) {
            throw new ProductValidationException("Product price must be greater than 0");
        }
        if (productDTO.getStock() == null || productDTO.getStock() < 0) {
            throw new ProductValidationException("Product stock cannot be negative");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}

