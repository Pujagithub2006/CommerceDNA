package io.commercedna.settlement.dto;

public record PaymentLinkResponse(
        String orderCode,
        String paymentLinkId,
        String paymentLinkUrl,
        long amountPaise,
        String currency,
        String status
) {}
