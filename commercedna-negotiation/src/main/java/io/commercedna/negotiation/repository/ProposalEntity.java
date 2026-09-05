package io.commercedna.negotiation.repository;

import io.commercedna.core.entity.ProposalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "proposals",
        indexes = {
                @Index(name = "idx_proposals_code", columnList = "proposal_code", unique = true),
                @Index(name = "idx_proposals_merchant", columnList = "merchant_id"),
                @Index(name = "idx_proposals_buyer", columnList = "buyer_agent_did"),
                @Index(name = "idx_proposals_status", columnList = "status")
        }
)
public class ProposalEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proposal_code", nullable = false, unique = true, length = 64)
    private String proposalCode;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "buyer_agent_did", nullable = false, length = 128)
    private String buyerAgentDid;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "proposed_unit_price_paise", nullable = false)
    private Long proposedUnitPricePaise;

    @Column(name = "counter_unit_price_paise")
    private Long counterUnitPricePaise;

    @Column(name = "total_amount_paise", nullable = false)
    private Long totalAmountPaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProposalStatus status;

    @Column(name = "rationale", length = 1024)
    private String rationale;

    @Column(name = "buyer_public_key", length = 512)
    private String buyerPublicKey;

    @Column(name = "buyer_signature", length = 512)
    private String buyerSignature;

    @Column(name = "merchant_signature", length = 512)
    private String merchantSignature;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProposalEntity() {
    }

    public ProposalEntity(
            UUID id,
            String proposalCode,
            UUID merchantId,
            String buyerAgentDid,
            String sku,
            Integer quantity,
            Long proposedUnitPricePaise,
            Long counterUnitPricePaise,
            Long totalAmountPaise,
            String currency,
            ProposalStatus status,
            String rationale,
            String buyerPublicKey,
            String buyerSignature,
            String merchantSignature,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.proposalCode = proposalCode;
        this.merchantId = merchantId;
        this.buyerAgentDid = buyerAgentDid;
        this.sku = sku;
        this.quantity = quantity;
        this.proposedUnitPricePaise = proposedUnitPricePaise;
        this.counterUnitPricePaise = counterUnitPricePaise;
        this.totalAmountPaise = totalAmountPaise;
        this.currency = currency;
        this.status = status;
        this.rationale = rationale;
        this.buyerPublicKey = buyerPublicKey;
        this.buyerSignature = buyerSignature;
        this.merchantSignature = merchantSignature;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProposalCode() {
        return proposalCode;
    }

    public void setProposalCode(String proposalCode) {
        this.proposalCode = proposalCode;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public String getBuyerAgentDid() {
        return buyerAgentDid;
    }

    public void setBuyerAgentDid(String buyerAgentDid) {
        this.buyerAgentDid = buyerAgentDid;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getProposedUnitPricePaise() {
        return proposedUnitPricePaise;
    }

    public void setProposedUnitPricePaise(Long proposedUnitPricePaise) {
        this.proposedUnitPricePaise = proposedUnitPricePaise;
    }

    public Long getCounterUnitPricePaise() {
        return counterUnitPricePaise;
    }

    public void setCounterUnitPricePaise(Long counterUnitPricePaise) {
        this.counterUnitPricePaise = counterUnitPricePaise;
    }

    public Long getTotalAmountPaise() {
        return totalAmountPaise;
    }

    public void setTotalAmountPaise(Long totalAmountPaise) {
        this.totalAmountPaise = totalAmountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getBuyerPublicKey() {
        return buyerPublicKey;
    }

    public void setBuyerPublicKey(String buyerPublicKey) {
        this.buyerPublicKey = buyerPublicKey;
    }

    public String getBuyerSignature() {
        return buyerSignature;
    }

    public void setBuyerSignature(String buyerSignature) {
        this.buyerSignature = buyerSignature;
    }

    public String getMerchantSignature() {
        return merchantSignature;
    }

    public void setMerchantSignature(String merchantSignature) {
        this.merchantSignature = merchantSignature;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
