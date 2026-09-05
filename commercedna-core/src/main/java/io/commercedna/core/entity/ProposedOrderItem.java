package io.commercedna.core.entity;

import io.commercedna.core.exception.DomainException;
import io.commercedna.core.model.Money;

import java.io.Serializable;
import java.util.Objects;

/**
 * Line item in an agentic IntentProposal.
 */
public final class ProposedOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sku;
    private final int quantity;
    private final Money negotiatedUnitPrice;

    public ProposedOrderItem(String sku, int quantity, Money negotiatedUnitPrice) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new DomainException("ProposedOrderItem SKU must not be blank.");
        }
        if (quantity <= 0) {
            throw new DomainException("ProposedOrderItem quantity must be strictly positive: " + quantity);
        }
        this.sku = sku.trim();
        this.quantity = quantity;
        this.negotiatedUnitPrice = Objects.requireNonNull(negotiatedUnitPrice, "negotiatedUnitPrice must not be null");
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getNegotiatedUnitPrice() {
        return negotiatedUnitPrice;
    }

    public Money calculateSubtotal() {
        return negotiatedUnitPrice.multiply(quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProposedOrderItem that = (ProposedOrderItem) o;
        return quantity == that.quantity &&
                Objects.equals(sku, that.sku) &&
                Objects.equals(negotiatedUnitPrice, that.negotiatedUnitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, quantity, negotiatedUnitPrice);
    }

    @Override
    public String toString() {
        return "ProposedOrderItem{" +
                "sku='" + sku + '\'' +
                ", quantity=" + quantity +
                ", negotiatedUnitPrice=" + negotiatedUnitPrice +
                '}';
    }
}
