package io.commercedna.core.model;

import io.commercedna.core.exception.DomainException;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an idempotency key (UUIDv4) required for state-mutating requests.
 */
public final class IdempotencyKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID value;

    public IdempotencyKey(UUID value) {
        this.value = Objects.requireNonNull(value, "Idempotency key value must not be null");
    }

    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID());
    }

    public static IdempotencyKey fromString(String key) {
        Objects.requireNonNull(key, "Idempotency-Key header value must not be null");
        try {
            return new IdempotencyKey(UUID.fromString(key.trim()));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Invalid Idempotency-Key format. Must be a valid UUIDv4: " + key, e);
        }
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
