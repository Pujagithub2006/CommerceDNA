package io.commercedna.core.exception;

/**
 * Thrown when an agent attempts to purchase outside the merchant's allowable quota limits.
 */
public class QuantityQuotaExceededException extends DomainException {

    public QuantityQuotaExceededException(String sku, int requestedQuantity, int maxAllowedQuantity) {
        super(
            String.format("Requested quantity %d exceeds maximum allowable single-buyer quota of %d for SKU '%s'",
                requestedQuantity, maxAllowedQuantity, sku),
            "CDNA_POLICY_QUANTITY_EXCEEDED"
        );
    }

    public QuantityQuotaExceededException(String message) {
        super(message, "CDNA_POLICY_QUANTITY_EXCEEDED");
    }
}
