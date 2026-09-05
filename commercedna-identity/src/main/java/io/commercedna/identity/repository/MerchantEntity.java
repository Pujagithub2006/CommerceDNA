package io.commercedna.identity.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class MerchantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "merchant_code", nullable = false, unique = true, length = 64)
    private String merchantCode;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "public_key_ed25519", nullable = false, length = 128)
    private String publicKeyEd25519;

    @Column(name = "encrypted_private_key_ed25519", columnDefinition = "TEXT")
    private String encryptedPrivateKeyEd25519;

    @Column(name = "encrypted_razorpay_key_id", nullable = false, columnDefinition = "TEXT")
    private String encryptedRazorpayKeyId;

    @Column(name = "encrypted_razorpay_key_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedRazorpayKeySecret;

    @Column(name = "webhook_secret", nullable = false, columnDefinition = "TEXT")
    private String webhookSecret;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MerchantEntity() {
    }

    public MerchantEntity(
            UUID id,
            String merchantCode,
            String businessName,
            String contactEmail,
            String publicKeyEd25519,
            String encryptedRazorpayKeyId,
            String encryptedRazorpayKeySecret,
            String webhookSecret,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.merchantCode = merchantCode;
        this.businessName = businessName;
        this.contactEmail = contactEmail;
        this.publicKeyEd25519 = publicKeyEd25519;
        this.encryptedRazorpayKeyId = encryptedRazorpayKeyId;
        this.encryptedRazorpayKeySecret = encryptedRazorpayKeySecret;
        this.webhookSecret = webhookSecret;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPublicKeyEd25519() {
        return publicKeyEd25519;
    }

    public void setPublicKeyEd25519(String publicKeyEd25519) {
        this.publicKeyEd25519 = publicKeyEd25519;
    }

    public String getEncryptedPrivateKeyEd25519() {
        return encryptedPrivateKeyEd25519;
    }

    public void setEncryptedPrivateKeyEd25519(String encryptedPrivateKeyEd25519) {
        this.encryptedPrivateKeyEd25519 = encryptedPrivateKeyEd25519;
    }

    public String getEncryptedRazorpayKeyId() {
        return encryptedRazorpayKeyId;
    }

    public void setEncryptedRazorpayKeyId(String encryptedRazorpayKeyId) {
        this.encryptedRazorpayKeyId = encryptedRazorpayKeyId;
    }

    public String getEncryptedRazorpayKeySecret() {
        return encryptedRazorpayKeySecret;
    }

    public void setEncryptedRazorpayKeySecret(String encryptedRazorpayKeySecret) {
        this.encryptedRazorpayKeySecret = encryptedRazorpayKeySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
