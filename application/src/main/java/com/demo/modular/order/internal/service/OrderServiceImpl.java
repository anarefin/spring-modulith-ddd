package com.demo.modular.order.internal.service;

import com.demo.modular.order.api.dto.CreateOrderRequest;
import com.demo.modular.order.api.dto.OrderDTO;
import com.demo.modular.order.api.dto.OrderStatus;
import com.demo.modular.order.api.exception.OrderCreationException;
import com.demo.modular.order.api.exception.OrderInvalidStateException;
import com.demo.modular.order.api.exception.OrderNotFoundException;
import com.demo.modular.order.internal.domain.Order;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
        validateCreateOrderRequest(request);
        
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
            
            // Calculate total amount
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            
            // Create order
            Order order = new Order();
            order.setProductId(productId);
            order.setProductName(product.getName()); // Store snapshot
            order.setQuantity(quantity);
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING);
            
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
                    savedOrder.setStatus(OrderStatus.CANCELLED);
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
        validateId(id);
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
        validateId(orderId);
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        
        log.info("[Order Module] [Inter-Module Call] Updating order {} status to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate status transition
        validateStatusTransition(order.getStatus(), status);
        
        order.setStatus(status);
        orderRepository.save(order);
        log.info("[Order Module] Order status updated successfully");
    }

    @Override
    @Timed(value = "order.cancel", description = "Time taken to cancel an order")
    public void cancelOrder(Long id) {
        validateId(id);
        log.info("[Order Module] Cancelling order with id: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        
        // Validate that order can be cancelled
        if (order.getStatus() == OrderStatus.PAID) {
            throw new OrderInvalidStateException(id, order.getStatus(), "Cannot cancel paid order");
        }
        
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("[Order Module] Order {} is already cancelled", id);
            return;
        }
        
        // Compensation: Restore stock if order was pending
        if (order.getStatus() == OrderStatus.PENDING) {
            try {
                log.info("[Order Module] [Compensation] Restoring stock for cancelled order {}", id);
                productService.restoreStock(order.getProductId(), order.getQuantity());
                log.info("[Order Module] [Compensation] Stock restored successfully");
            } catch (Exception e) {
                log.error("[Order Module] [Compensation] Failed to restore stock for order {}", id, e);
                // Continue with cancellation even if stock restoration fails
            }
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[Order Module] Order cancelled successfully");
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateOrderRequest cannot be null");
        }
        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid status transitions
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.FAILED || newStatus == OrderStatus.CANCELLED;
            case PAID -> false; // Cannot transition from PAID
            case FAILED -> newStatus == OrderStatus.CANCELLED;
            case CANCELLED -> false; // Cannot transition from CANCELLED;
        };
        
        if (!isValid) {
            throw new OrderInvalidStateException(null, currentStatus, 
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }
    }
}

