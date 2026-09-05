package io.commercedna.negotiation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NegotiateChatRequest(
        @NotBlank(message = "merchantCode is required")
        String merchantCode,

        @NotBlank(message = "buyerAgentDid is required")
        String buyerAgentDid,

        @NotBlank(message = "sku is required")
        String sku,

        @NotBlank(message = "buyerMessage is required")
        String buyerMessage,

        String buyerPublicKey,
        String buyerSignature
) {}
