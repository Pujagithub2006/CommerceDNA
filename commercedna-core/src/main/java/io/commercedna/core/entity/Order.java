package io.commercedna.core.entity;

import io.commercedna.core.exception.DomainException;
import io.commercedna.core.model.IdempotencyKey;
import io.commercedna.core.model.Money;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Order Domain Entity.
 * Represents an airgap-validated, actionable commercial contract mapped to Razorpay Test Mode settlement rails.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String orderCode;
    private final UUID proposalId;
    private final UUID merchantId;
    private final String buyerAgentId;
    private String razorpayOrderId;
    private String razorpayPaymentLinkId;
    private String razorpayPaymentLinkUrl;
    private final Money amount;
    private OrderStatus status;
    private final IdempotencyKey idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(
            UUID id,
            String orderCode,
            UUID proposalId,
            UUID merchantId,
            String buyerAgentId,
            String razorpayOrderId,
            String razorpayPaymentLinkId,
            String razorpayPaymentLinkUrl,
            Money amount,
            OrderStatus status,
            IdempotencyKey idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Order ID must not be null");
        this.orderCode = Objects.requireNonNull(orderCode, "orderCode must not be null");
        this.proposalId = Objects.requireNonNull(proposalId, "proposalId must not be null");
        this.merchantId = Objects.requireNonNull(merchantId, "merchantId must not be null");
        this.buyerAgentId = Objects.requireNonNull(buyerAgentId, "buyerAgentId must not be null");
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentLinkId = razorpayPaymentLinkId;
        this.razorpayPaymentLinkUrl = razorpayPaymentLinkUrl;
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Order createFromProposal(
            IntentProposal proposal,
            IdempotencyKey idempotencyKey
    ) {
        if (proposal.getStatus() != ProposalStatus.APPROVED) {
            throw new DomainException("Cannot create order from unapproved proposal: " + proposal.getStatus());
        }
        Instant now = Instant.now();
        String code = "ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new Order(
                UUID.randomUUID(),
                code,
                proposal.getId(),
                proposal.getMerchantId(),
                proposal.getBuyerAgentId(),
                null,
                null,
                null,
                proposal.getTotalAmount(),
                OrderStatus.CREATED,
                idempotencyKey,
                now,
                now
        );
    }

    public void attachRazorpayOrder(String razorpayOrderId) {
        this.razorpayOrderId = Objects.requireNonNull(razorpayOrderId, "razorpayOrderId must not be null");
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.updatedAt = Instant.now();
    }

    public void attachRazorpayPaymentLink(String paymentLinkId, String shortUrl) {
        this.razorpayPaymentLinkId = Objects.requireNonNull(paymentLinkId, "paymentLinkId must not be null");
        this.razorpayPaymentLinkUrl = Objects.requireNonNull(shortUrl, "shortUrl must not be null");
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.updatedAt = Instant.now();
    }

    public void markPaid() {
        if (this.status == OrderStatus.PAID) {
            return; // Idempotent
        }
        if (this.status != OrderStatus.AWAITING_PAYMENT && this.status != OrderStatus.CREATED) {
            throw new DomainException("Cannot mark order PAID from status: " + this.status);
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        if (this.status != OrderStatus.PAID) {
            throw new DomainException("Cannot refund unpaid order in status: " + this.status);
        }
        this.status = OrderStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getBuyerAgentId() {
        return buyerAgentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentLinkId() {
        return razorpayPaymentLinkId;
    }

    public String getRazorpayPaymentLinkUrl() {
        return razorpayPaymentLinkUrl;
    }

    public Money getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
