@org.springframework.modulith.ApplicationModule(
    displayName = "Product Module",
    allowedDependencies = {}
)
package com.demo.modular.product;

/**
 * Product Module - Manages product catalog and inventory.
 * 
 * <p><b>Module Structure:</b> Uses explicit internal package for better encapsulation.</p>
 * 
 * <p><b>Public API (accessible from other modules):</b></p>
 * <ul>
 *   <li>api.dto - DTOs for cross-module communication (ProductDTO, StockCheckDTO)</li>
 *   <li>api.exception - Public exceptions (ProductNotFoundException, InsufficientStockException)</li>
 *   <li>service - Service interfaces for inter-module communication (ProductService)</li>
 * </ul>
 * 
 * <p><b>Internal Packages (module-private):</b></p>
 * <ul>
 *   <li>internal.domain - JPA entities (Product)</li>
 *   <li>internal.repository - Data access layer (ProductRepository)</li>
 *   <li>internal.service - Service implementations and mappers (ProductServiceImpl, ProductMapper)</li>
 * </ul>
 * 
 * <p><b>Dependencies:</b> None - This is a foundational module with no dependencies on other modules.</p>
 * 
 * <p><b>Best Practice:</b> The explicit `internal` package makes module boundaries crystal clear
 * and prevents accidental exposure of internal implementation details.</p>
 */

