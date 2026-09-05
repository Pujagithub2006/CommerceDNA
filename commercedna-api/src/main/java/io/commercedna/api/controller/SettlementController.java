package io.commercedna.api.controller;

import io.commercedna.settlement.dto.CreateOrderRequest;
import io.commercedna.settlement.dto.CreateOrderResponse;
import io.commercedna.settlement.dto.CreatePaymentLinkRequest;
import io.commercedna.settlement.dto.PaymentLinkResponse;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/settlement")
@Tag(name = "Settlement & Payment Gateway", description = "Atomic order creation, Razorpay test mode settlement, and payment link generation")
public class SettlementController {

    private final OrderService orderService;

    public SettlementController(OrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }

    @PostMapping("/orders")
    @Operation(summary = "Create an order atomically from an approved deal proposal or direct transaction with Idempotency Key")
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders/{code}")
    @Operation(summary = "Fetch order details and current settlement status by order code")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable("code") String code) {
        OrderEntity order = orderService.getOrderByCode(code);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/payment-links")
    @Operation(summary = "Generate or retrieve a Razorpay hosted payment link for an order")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(@Valid @RequestBody CreatePaymentLinkRequest request) {
        PaymentLinkResponse response = orderService.createPaymentLink(request);
        return ResponseEntity.ok(response);
    }
}
