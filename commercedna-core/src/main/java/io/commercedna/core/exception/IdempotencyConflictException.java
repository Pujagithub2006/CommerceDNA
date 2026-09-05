package io.commercedna.core.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String idempotencyKey) {
        super(
            "A request with Idempotency-Key '" + idempotencyKey + "' is currently in progress or encountered a conflict.",
            "CDNA_IDEMPOTENCY_CONFLICT"
        );
    }
}
