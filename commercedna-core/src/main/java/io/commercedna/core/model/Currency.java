package io.commercedna.core.model;

/**
 * Supported currencies in CommerceDNA.
 * Primary settlement currency for Razorpay Test Mode is INR.
 */
public enum Currency {
    INR("Indian Rupee", "₹", 100),
    USD("United States Dollar", "$", 100);

    private final String description;
    private final String symbol;
    private final int subunitFactor;

    Currency(String description, String symbol, int subunitFactor) {
        this.description = description;
        this.symbol = symbol;
        this.subunitFactor = subunitFactor;
    }

    public String getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSubunitFactor() {
        return subunitFactor;
    }
}
