package io.commercedna.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,

        @NotBlank(message = "merchantCode is required")
        String merchantCode,

        @NotBlank(message = "buyerAgentDid is required")
        String buyerAgentDid,

        String proposalCode,

        @NotBlank(message = "sku is required")
        String sku,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be positive")
        Integer quantity,

        @NotNull(message = "unitPricePaise is required")
        @Positive(message = "unitPricePaise must be positive")
        Long unitPricePaise
) {}
