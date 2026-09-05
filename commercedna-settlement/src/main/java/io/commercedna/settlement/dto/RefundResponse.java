package io.commercedna.settlement.dto;

import io.commercedna.core.entity.OrderStatus;

public record RefundResponse(
        String orderCode,
        String refundId,
        String razorpayPaymentId,
        long refundedAmountPaise,
        String currency,
        OrderStatus status,
        String message
) {}
