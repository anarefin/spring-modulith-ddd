package com.demo.modular.payment.api;

import com.demo.modular.payment.api.dto.PaymentDTO;
import com.demo.modular.payment.api.dto.PaymentStatus;
import com.demo.modular.payment.api.exception.DuplicatePaymentException;
import com.demo.modular.payment.api.exception.PaymentProcessingException;
import com.demo.modular.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDTO> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        log.info("REST: Processing payment for order {} with method {}", 
                request.getOrderId(), request.getPaymentMethod());
        try {
            PaymentDTO payment = paymentService.processPayment(request.getOrderId(), request.getPaymentMethod());
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (DuplicatePaymentException e) {
            log.error("Duplicate payment attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (PaymentProcessingException e) {
            log.error("Failed to process payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long id) {
        log.info("REST: Getting payment {}", id);
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDTO> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("REST: Getting payment for order {}", orderId);
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        log.info("REST: Getting all payments");
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        log.info("REST: Getting payments with status {}", status);
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @Data
    public static class ProcessPaymentRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;
        
        @NotBlank(message = "Payment method is required")
        private String paymentMethod;
    }
}

