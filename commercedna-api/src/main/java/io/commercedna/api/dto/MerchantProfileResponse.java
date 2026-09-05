package io.commercedna.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MerchantProfileResponse(
        UUID id,
        String merchantCode,
        String businessName,
        String contactEmail,
        String merchantDid,
        String publicKeyEd25519,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
