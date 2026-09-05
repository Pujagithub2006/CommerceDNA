package io.commercedna.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentLinkRequest(
        @NotBlank(message = "merchantCode is required")
        String merchantCode,

        @NotBlank(message = "orderCode is required")
        String orderCode,

        String customerName,
        String customerEmail
) {}
