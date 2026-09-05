package io.commercedna.core.exception;

/**
 * Thrown when an agentic proposal breaches the merchant's minimum profit margin floor.
 */
public class MarginFloorViolationException extends DomainException {

    private final String sku;
    private final long proposedUnitPricePaise;
    private final long floorPricePaise;

    public MarginFloorViolationException(String sku, long proposedUnitPricePaise, long floorPricePaise) {
        super(
            String.format(
                "Negotiated price %d paise is below the allowable floor of %d paise for SKU '%s'.",
                proposedUnitPricePaise, floorPricePaise, sku
            ),
            "CDNA_POLICY_MARGIN_VIOLATION"
        );
        this.sku = sku;
        this.proposedUnitPricePaise = proposedUnitPricePaise;
        this.floorPricePaise = floorPricePaise;
    }

    public String getSku() {
        return sku;
    }

    public long getProposedUnitPricePaise() {
        return proposedUnitPricePaise;
    }

    public long getFloorPricePaise() {
        return floorPricePaise;
    }

    public long getAllowableFloorPaise() {
        return floorPricePaise;
    }
}

