package io.commercedna.core.entity;

import io.commercedna.core.exception.DomainException;
import io.commercedna.core.model.MerchantDID;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sovereign Merchant Domain Entity.
 * Represents a business on CommerceDNA with an AI-native cryptographic identity.
 */
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String merchantCode;
    private final String businessName;
    private final String contactEmail;
    private final String publicKeyEd25519;
    private final MerchantDID did;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Merchant(
            UUID id,
            String merchantCode,
            String businessName,
            String contactEmail,
            String publicKeyEd25519,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Merchant ID must not be null");
        this.merchantCode = validateNonBlank(merchantCode, "merchantCode");
        this.businessName = validateNonBlank(businessName, "businessName");
        this.contactEmail = validateNonBlank(contactEmail, "contactEmail");
        this.publicKeyEd25519 = validateNonBlank(publicKeyEd25519, "publicKeyEd25519");
        this.did = MerchantDID.fromMerchantCode(merchantCode);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Merchant createNew(
            String merchantCode,
            String businessName,
            String contactEmail,
            String publicKeyEd25519
    ) {
        Instant now = Instant.now();
        return new Merchant(
                UUID.randomUUID(),
                merchantCode,
                businessName,
                contactEmail,
                publicKeyEd25519,
                true,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getPublicKeyEd25519() {
        return publicKeyEd25519;
    }

    public MerchantDID getDid() {
        return did;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    private static String validateNonBlank(String val, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new DomainException("Merchant field '" + fieldName + "' must not be blank.");
        }
        return val.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchant merchant = (Merchant) o;
        return Objects.equals(id, merchant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "id=" + id +
                ", merchantCode='" + merchantCode + '\'' +
                ", businessName='" + businessName + '\'' +
                ", did=" + did +
                ", active=" + active +
                '}';
    }
}
