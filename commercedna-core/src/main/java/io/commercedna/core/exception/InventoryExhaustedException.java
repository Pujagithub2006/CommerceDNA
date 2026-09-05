package io.commercedna.core.exception;

public class InventoryExhaustedException extends DomainException {
    public InventoryExhaustedException(String sku, int requestedQuantity, int availableQuantity) {
        super(
            String.format("Insufficient inventory for SKU '%s'. Requested: %d, Available: %d",
                sku, requestedQuantity, availableQuantity),
            "CDNA_INVENTORY_EXHAUSTED"
        );
    }
}
