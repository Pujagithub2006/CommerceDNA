package io.commercedna.core.entity;

import io.commercedna.core.exception.DomainException;
import io.commercedna.core.model.Money;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical IntentProposal domain entity.
 * Represents an immutable, cryptographically signed deal proposal compiled from agentic negotiation.
 */
public class IntentProposal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String proposalCode;
    private final UUID sessionId;
    private final UUID merchantId;
    private final String buyerAgentId;
    private final List<ProposedOrderItem> items;
    private final Money totalAmount;
    private final BigDecimal appliedDiscountPercent;
    private final String buyerSignatureEd25519;
    private ProposalStatus status;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public IntentProposal(
            UUID id,
            String proposalCode,
            UUID sessionId,
            UUID merchantId,
            String buyerAgentId,
            List<ProposedOrderItem> items,
            Money totalAmount,
            BigDecimal appliedDiscountPercent,
            String buyerSignatureEd25519,
            ProposalStatus status,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Proposal ID must not be null");
        this.proposalCode = Objects.requireNonNull(proposalCode, "proposalCode must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.merchantId = Objects.requireNonNull(merchantId, "merchantId must not be null");
        this.buyerAgentId = Objects.requireNonNull(buyerAgentId, "buyerAgentId must not be null");
        if (items == null || items.isEmpty()) {
            throw new DomainException("IntentProposal must contain at least one item.");
        }
        this.items = List.copyOf(items);
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.appliedDiscountPercent = Objects.requireNonNull(appliedDiscountPercent, "appliedDiscountPercent must not be null");
        this.buyerSignatureEd25519 = buyerSignatureEd25519;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.rejectionReason = rejectionReason;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static IntentProposal createPending(
            UUID sessionId,
            UUID merchantId,
            String buyerAgentId,
            List<ProposedOrderItem> items,
            BigDecimal appliedDiscountPercent,
            String buyerSignatureEd25519
    ) {
        Money computedTotal = items.stream()
                .map(ProposedOrderItem::calculateSubtotal)
                .reduce(Money.zero(), Money::plus);

        Instant now = Instant.now();
        String code = "prp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new IntentProposal(
                UUID.randomUUID(),
                code,
                sessionId,
                merchantId,
                buyerAgentId,
                items,
                computedTotal,
                appliedDiscountPercent,
                buyerSignatureEd25519,
                ProposalStatus.PENDING,
                null,
                now,
                now
        );
    }

    public void approve() {
        if (this.status != ProposalStatus.PENDING) {
            throw new DomainException("Cannot approve proposal in status: " + this.status);
        }
        this.status = ProposalStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status != ProposalStatus.PENDING) {
            throw new DomainException("Cannot reject proposal in status: " + this.status);
        }
        this.status = ProposalStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProposalCode() {
        return proposalCode;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getBuyerAgentId() {
        return buyerAgentId;
    }

    public List<ProposedOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getAppliedDiscountPercent() {
        return appliedDiscountPercent;
    }

    public String getBuyerSignatureEd25519() {
        return buyerSignatureEd25519;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
