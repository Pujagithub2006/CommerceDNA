package io.commercedna.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterMerchantResponse(
        UUID merchantId,
        String merchantCode,
        String businessName,
        String contactEmail,
        String merchantDid,
        String publicKeyEd25519,
        String privateKeyEd25519,
        Instant createdAt
) {}
