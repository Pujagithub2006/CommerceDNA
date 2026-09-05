package io.commercedna.negotiation.dto;

import io.commercedna.core.entity.ProposalStatus;

public record NegotiateChatResponse(
        String sessionId,
        String merchantCode,
        String sku,
        String agentReply,
        ProposalStatus proposalStatus,
        String proposalCode,
        Long unitPricePaise,
        Integer quantity,
        Long totalAmountPaise,
        String rationale,
        String merchantSignature,
        boolean airgapViolation
) {}
