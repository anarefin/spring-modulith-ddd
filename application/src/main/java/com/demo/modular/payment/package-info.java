@org.springframework.modulith.ApplicationModule(
    displayName = "Payment Module",
    allowedDependencies = "order"
)
package com.demo.modular.payment;

/**
 * Payment Module - Processes payments for orders.
 * 
 * <p><b>Module Structure:</b> Uses explicit internal package for better encapsulation.</p>
 * 
 * <p><b>Public API (accessible from other modules):</b></p>
 * <ul>
 *   <li>api.dto - DTOs and enums (PaymentDTO, PaymentStatus)</li>
 *   <li>api.exception - Public exceptions (PaymentNotFoundException, PaymentProcessingException, DuplicatePaymentException)</li>
 *   <li>service - Service interfaces for inter-module communication (PaymentService)</li>
 * </ul>
 * 
 * <p><b>Internal Packages (module-private):</b></p>
 * <ul>
 *   <li>internal.domain - JPA entities (Payment)</li>
 *   <li>internal.repository - Data access layer (PaymentRepository)</li>
 *   <li>internal.service - Service implementations and mappers (PaymentServiceImpl, PaymentMapper)</li>
 * </ul>
 * 
 * <p><b>Dependencies:</b></p>
 * <ul>
 *   <li>order - Uses OrderService to validate orders and update order status based on payment result</li>
 * </ul>
 * 
 * <p><b>Best Practice:</b> The explicit `internal` package makes module boundaries crystal clear
 * and prevents accidental exposure of internal implementation details.</p>
 */

