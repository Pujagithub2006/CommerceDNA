package io.commercedna.settlement.dto;

import io.commercedna.core.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String orderCode,
        String merchantCode,
        String buyerAgentDid,
        String proposalCode,
        String sku,
        int quantity,
        long unitPricePaise,
        long totalAmountPaise,
        String currency,
        OrderStatus status,
        String razorpayOrderId,
        String paymentLinkUrl,
        Instant createdAt
) {}
