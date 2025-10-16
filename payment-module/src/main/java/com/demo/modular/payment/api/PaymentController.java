package com.demo.modular.payment.api;

import com.demo.modular.payment.domain.Payment;
import com.demo.modular.payment.domain.PaymentStatus;
import com.demo.modular.payment.service.PaymentService;
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
    public ResponseEntity<Payment> processPayment(@RequestBody ProcessPaymentRequest request) {
        log.info("REST: Processing payment for order {} with method {}", 
                request.getOrderId(), request.getPaymentMethod());
        try {
            Payment payment = paymentService.processPayment(request.getOrderId(), request.getPaymentMethod());
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (RuntimeException e) {
            log.error("Failed to process payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        log.info("REST: Getting payment {}", id);
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("REST: Getting payment for order {}", orderId);
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        log.info("REST: Getting all payments");
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        log.info("REST: Getting payments with status {}", status);
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @Data
    public static class ProcessPaymentRequest {
        private Long orderId;
        private String paymentMethod;
    }
}

