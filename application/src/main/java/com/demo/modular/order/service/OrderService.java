package com.demo.modular.order.service;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.api.exception.OrderCreationException;
import com.demo.modular.order.api.exception.OrderInvalidStateException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Order Service Interface - Public API for inter-module communication.
 * 
 * <p>This interface defines the contract for order management operations.
 * All methods use DTOs to prevent domain model exposure to other modules.</p>
 * 
 * <p><b>Transaction Boundaries:</b> Order creation involves cross-module calls
 * to Product module. Each service manages its own transaction.</p>
 * 
 * <p><b>Thread Safety:</b> All implementations must be thread-safe.</p>
 */
public interface OrderService {
    
    /**
     * Creates a new order for a product.
     * 
     * <p><b>Inter-Module Dependencies:</b></p>
     * <ul>
     *   <li>Calls ProductService.checkStockAvailability() to verify stock</li>
     *   <li>Calls ProductService.reduceStock() to decrement stock</li>
     * </ul>
     * 
     * <p><b>Compensation:</b> If order creation fails after stock reduction,
     * compensation logic will call ProductService.restoreStock().</p>
     * 
     * <p><b>Transaction:</b> Runs in its own transaction. Stock reduction happens
     * in a separate transaction (REQUIRES_NEW propagation).</p>
     * 
     * @param request the order creation request containing productId and quantity
     * @return the created order with PENDING status
     * @throws OrderCreationException if order creation fails
     * @throws IllegalArgumentException if request is null or invalid
     */
    OrderDTO createOrder(@Valid @NotNull CreateOrderRequest request);
    
    /**
     * Retrieves an order by its ID.
     * 
     * <p><b>Inter-Module Usage:</b> Called by Payment module to validate orders.</p>
     * 
     * @param id the order ID
     * @return Optional containing the order if found, empty otherwise
     * @throws IllegalArgumentException if id is null or negative
     */
    Optional<OrderDTO> getOrderById(@NotNull Long id);
    
    /**
     * Retrieves all orders in the system.
     * 
     * @return list of all orders, empty list if none exist
     */
    List<OrderDTO> getAllOrders();
    
    /**
     * Retrieves orders by status.
     * 
     * @param status the order status to filter by
     * @return list of orders with the given status, empty list if none exist
     * @throws IllegalArgumentException if status is null
     */
    List<OrderDTO> getOrdersByStatus(@NotNull OrderStatus status);
    
    /**
     * Updates order status.
     * 
     * <p><b>Inter-Module Usage:</b> Called by Payment module to update order
     * status to PAID or FAILED based on payment result.</p>
     * 
     * <p><b>Transaction:</b> Runs in REQUIRES_NEW propagation to ensure
     * independent transaction boundary.</p>
     * 
     * @param orderId the order ID to update
     * @param status the new order status
     * @throws OrderNotFoundException if order doesn't exist
     * @throws OrderInvalidStateException if status transition is invalid
     * @throws IllegalArgumentException if parameters are null
     */
    void updateOrderStatus(@NotNull Long orderId, @NotNull OrderStatus status);
    
    /**
     * Cancels an order.
     * 
     * <p><b>Compensation:</b> If order has already reduced stock, this method
     * should trigger stock restoration.</p>
     * 
     * @param id the order ID to cancel
     * @throws OrderNotFoundException if order doesn't exist
     * @throws OrderInvalidStateException if order cannot be cancelled
     * @throws IllegalArgumentException if id is null
     */
    void cancelOrder(@NotNull Long id);
}

