package com.demo.modular.order.internal.service;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.api.exception.OrderCreationException;
import com.demo.modular.order.api.exception.OrderInvalidStateException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import com.demo.modular.order.internal.domain.Order;
import com.demo.modular.order.internal.domain.vo.Money;
import com.demo.modular.order.internal.domain.vo.ProductId;
import com.demo.modular.order.internal.domain.vo.ProductName;
import com.demo.modular.order.internal.domain.vo.Quantity;
import com.demo.modular.order.internal.repository.OrderRepository;
import com.demo.modular.order.service.OrderService;
import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.api.dto.StockCheckDTO;
import com.demo.modular.product.api.exception.InsufficientStockException;
import com.demo.modular.product.api.exception.ProductNotFoundException;
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
 * Application Service for Order module.
 * Orchestrates use cases - delegates business logic to domain layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService; // Inter-module dependency
    private final OrderMapper orderMapper;

    @Override
    @Timed(value = "order.create", description = "Time taken to create an order")
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("[Order Module] Creating order for product {} with quantity {}", 
                request.getProductId(), request.getQuantity());
        
        Long productId = request.getProductId();
        Integer quantity = request.getQuantity();
        Order savedOrder = null;
        boolean stockReduced = false;
        
        try {
            // Inter-module call: Get product details from Product module
            ProductDTO product = productService.getProductById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            
            log.debug("[Order Module] Product found: {}, price: {}", product.getName(), product.getPrice());
            
            // Inter-module call: Check stock availability
            StockCheckDTO stockCheck = productService.checkStockAvailability(productId, quantity);
            if (!stockCheck.isAvailable()) {
                throw new InsufficientStockException(productId, quantity, stockCheck.getAvailableStock());
            }
            
            // Calculate total amount using value objects
            Money unitPrice = Money.of(product.getPrice());
            Money totalAmount = unitPrice.multiply(quantity);
            
            // Use static factory method to create order aggregate with validation
            Order order = Order.create(
                ProductId.of(productId),
                ProductName.of(product.getName()),
                Quantity.of(quantity),
                totalAmount
            );
            
            savedOrder = orderRepository.save(order);
            log.info("[Order Module] Order saved with id: {}", savedOrder.getId());
            
            // Inter-module call: Reduce product stock
            // This runs in a separate transaction (REQUIRES_NEW)
            productService.reduceStock(productId, quantity);
            stockReduced = true;
            
            log.info("[Order Module] Order created successfully with id: {}", savedOrder.getId());
            return orderMapper.toDTO(savedOrder);
            
        } catch (ProductNotFoundException e) {
            log.error("[Order Module] Product not found: {}", productId, e);
            throw new OrderCreationException("Product not found: " + productId, e);
            
        } catch (InsufficientStockException e) {
            log.error("[Order Module] Insufficient stock for product: {}", productId, e);
            throw new OrderCreationException("Insufficient stock for product: " + productId, e);
            
        } catch (Exception e) {
            log.error("[Order Module] Failed to create order for product: {}", productId, e);
            
            // Compensation: Restore stock if it was reduced
            if (stockReduced) {
                try {
                    log.warn("[Order Module] [Compensation] Restoring stock for product {} due to order creation failure", productId);
                    productService.restoreStock(productId, quantity);
                    log.info("[Order Module] [Compensation] Stock restored successfully");
                } catch (Exception compensationEx) {
                    log.error("[Order Module] [Compensation] Failed to restore stock for product {}", productId, compensationEx);
                }
            }
            
            // Cancel order if it was saved
            if (savedOrder != null) {
                try {
                    log.warn("[Order Module] [Compensation] Marking order {} as cancelled", savedOrder.getId());
                    savedOrder.cancel();
                    orderRepository.save(savedOrder);
                } catch (Exception cancelEx) {
                    log.error("[Order Module] [Compensation] Failed to cancel order {}", savedOrder.getId(), cancelEx);
                }
            }
            
            throw new OrderCreationException("Failed to create order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "order.findById", description = "Time taken to find order by ID")
    public Optional<OrderDTO> getOrderById(Long id) {
        log.debug("[Order Module] Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .map(orderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "order.findAll", description = "Time taken to find all orders")
    public List<OrderDTO> getAllOrders() {
        log.debug("[Order Module] Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        return orderMapper.toDTOList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "order.findByStatus", description = "Time taken to find orders by status")
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        log.debug("[Order Module] Fetching orders with status: {}", status);
        List<Order> orders = orderRepository.findByStatus(status);
        return orderMapper.toDTOList(orders);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "order.updateStatus", description = "Time taken to update order status")
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        
        log.info("[Order Module] [Inter-Module Call] Updating order {} status to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Use aggregate's state machine methods
        try {
            switch (status) {
                case PAID -> order.markAsPaid();
                case FAILED -> order.markAsFailed();
                case CANCELLED -> order.cancel();
                default -> throw new IllegalArgumentException("Invalid status transition to: " + status);
            }
            
            orderRepository.save(order);
            log.info("[Order Module] Order status updated successfully");
        } catch (IllegalStateException e) {
            log.error("[Order Module] Failed to update order status", e);
            throw new OrderInvalidStateException(orderId, order.getStatus(), e.getMessage());
        }
    }

    @Override
    @Timed(value = "order.cancel", description = "Time taken to cancel an order")
    public void cancelOrder(Long id) {
        log.info("[Order Module] Cancelling order with id: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        
        // Use aggregate's business method to check if can be cancelled
        if (!order.canBeCancelled()) {
            throw new OrderInvalidStateException(id, order.getStatus(), 
                "Cannot cancel order in " + order.getStatus() + " state");
        }
        
        if (order.isCancelled()) {
            log.warn("[Order Module] Order {} is already cancelled", id);
            return;
        }
        
        // Compensation: Restore stock if order was pending
        if (order.isPending()) {
            try {
                log.info("[Order Module] [Compensation] Restoring stock for cancelled order {}", id);
                productService.restoreStock(order.getProductId(), order.getQuantity().getValue());
                log.info("[Order Module] [Compensation] Stock restored successfully");
            } catch (Exception e) {
                log.error("[Order Module] [Compensation] Failed to restore stock for order {}", id, e);
                // Continue with cancellation even if stock restoration fails
            }
        }
        
        // Use aggregate's state machine method
        order.cancel();
        orderRepository.save(order);
        log.info("[Order Module] Order cancelled successfully");
    }
}

