package io.commercedna.core.model;

import io.commercedna.core.exception.DomainException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Decentralized Identifier (DID) representing a sovereign merchant identity.
 * Format: did:cdna:merchant:<identifier>
 */
public final class MerchantDID implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern DID_PATTERN = Pattern.compile("^did:cdna:merchant:[a-zA-Z0-9_-]{4,64}$");

    private final String value;

    public MerchantDID(String value) {
        Objects.requireNonNull(value, "Merchant DID must not be null");
        String trimmed = value.trim();
        if (!DID_PATTERN.matcher(trimmed).matches()) {
            throw new DomainException("Invalid Merchant DID format: '" + trimmed + "'. Must match " + DID_PATTERN.pattern());
        }
        this.value = trimmed;
    }

    public static MerchantDID of(String value) {
        return new MerchantDID(value);
    }

    public static MerchantDID fromMerchantCode(String merchantCode) {
        Objects.requireNonNull(merchantCode, "Merchant code must not be null");
        return new MerchantDID("did:cdna:merchant:" + merchantCode.trim().toLowerCase());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MerchantDID that = (MerchantDID) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
