package io.commercedna.settlement.entity;

import io.commercedna.core.entity.OrderStatus;
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
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_code", columnList = "order_code", unique = true),
                @Index(name = "idx_orders_idempotency", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_orders_merchant", columnList = "merchant_id"),
                @Index(name = "idx_orders_rzp_order", columnList = "razorpay_order_id"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true, length = 64)
    private String orderCode;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "buyer_agent_did", nullable = false, length = 128)
    private String buyerAgentDid;

    @Column(name = "proposal_code", length = 64)
    private String proposalCode;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_paise", nullable = false)
    private Long unitPricePaise;

    @Column(name = "total_amount_paise", nullable = false)
    private Long totalAmountPaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "razorpay_order_id", length = 64)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 64)
    private String razorpayPaymentId;

    @Column(name = "razorpay_payment_link_id", length = 64)
    private String razorpayPaymentLinkId;

    @Column(name = "payment_link_url", length = 512)
    private String paymentLinkUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public OrderEntity() {
    }

    public OrderEntity(
            UUID id,
            String orderCode,
            UUID merchantId,
            String buyerAgentDid,
            String proposalCode,
            String idempotencyKey,
            String sku,
            Integer quantity,
            Long unitPricePaise,
            Long totalAmountPaise,
            String currency,
            OrderStatus status,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpayPaymentLinkId,
            String paymentLinkUrl,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.orderCode = orderCode;
        this.merchantId = merchantId;
        this.buyerAgentDid = buyerAgentDid;
        this.proposalCode = proposalCode;
        this.idempotencyKey = idempotencyKey;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPricePaise = unitPricePaise;
        this.totalAmountPaise = totalAmountPaise;
        this.currency = currency;
        this.status = status;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayPaymentLinkId = razorpayPaymentLinkId;
        this.paymentLinkUrl = paymentLinkUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
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

    public String getProposalCode() {
        return proposalCode;
    }

    public void setProposalCode(String proposalCode) {
        this.proposalCode = proposalCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public Long getUnitPricePaise() {
        return unitPricePaise;
    }

    public void setUnitPricePaise(Long unitPricePaise) {
        this.unitPricePaise = unitPricePaise;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayPaymentLinkId() {
        return razorpayPaymentLinkId;
    }

    public void setRazorpayPaymentLinkId(String razorpayPaymentLinkId) {
        this.razorpayPaymentLinkId = razorpayPaymentLinkId;
    }

    public String getPaymentLinkUrl() {
        return paymentLinkUrl;
    }

    public void setPaymentLinkUrl(String paymentLinkUrl) {
        this.paymentLinkUrl = paymentLinkUrl;
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
