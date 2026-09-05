package io.commercedna.settlement.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank(message = "orderCode is required")
        String orderCode,
        String reason
) {}
