@org.springframework.modulith.ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = "product"
)
package com.demo.modular.order;

/**
 * Order Module - Manages order creation and lifecycle.
 * 
 * <p><b>Module Structure:</b> Uses explicit internal package for better encapsulation.</p>
 * 
 * <p><b>Public API (accessible from other modules):</b></p>
 * <ul>
 *   <li>api.dto - DTOs and enums (OrderDTO, CreateOrderRequest, OrderStatus)</li>
 *   <li>api.exception - Public exceptions (OrderNotFoundException, OrderInvalidStateException, OrderCreationException)</li>
 *   <li>service - Service interfaces for inter-module communication (OrderService)</li>
 * </ul>
 * 
 * <p><b>Internal Packages (module-private):</b></p>
 * <ul>
 *   <li>internal.domain - JPA entities (Order)</li>
 *   <li>internal.repository - Data access layer (OrderRepository)</li>
 *   <li>internal.service - Service implementations and mappers (OrderServiceImpl, OrderMapper)</li>
 * </ul>
 * 
 * <p><b>Dependencies:</b></p>
 * <ul>
 *   <li>product - Uses ProductService to check stock, reduce inventory, and restore stock for compensation</li>
 * </ul>
 * 
 * <p><b>Note:</b> OrderStatus enum moved to api.dto since it's part of the public API contract
 * used by both OrderDTO and the Payment module.</p>
 */

