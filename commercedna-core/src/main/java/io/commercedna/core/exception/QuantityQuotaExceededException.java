package io.commercedna.core.exception;

/**
 * Thrown when an agent attempts to purchase more than the merchant's maximum single-buyer allocation.
 */
public class QuantityQuotaExceededException extends DomainException {

    public QuantityQuotaExceededException(String sku, int requestedQuantity, int maxAllowedQuantity) {
        super(
            String.format("Requested quantity %d exceeds maximum allowable single-buyer quota of %d for SKU '%s'",
                requestedQuantity, maxAllowedQuantity, sku),
            "CDNA_POLICY_QUANTITY_EXCEEDED"
        );
    }
}
