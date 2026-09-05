package io.commercedna.core.entity;

import io.commercedna.core.exception.DomainException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.model.Money;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Product Entity in the Merchant Catalog.
 * Encapsulates base price, cost price, deterministic margin floors, and stock management.
 */
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final UUID merchantId;
    private final String sku;
    private String title;
    private String description;
    private Money basePrice;
    private final Money costPrice;
    private BigDecimal minMarginPercent;
    private BigDecimal maxDiscountPercent;
    private int stockQuantity;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(
            UUID id,
            UUID merchantId,
            String sku,
            String title,
            String description,
            Money basePrice,
            Money costPrice,
            BigDecimal minMarginPercent,
            BigDecimal maxDiscountPercent,
            int stockQuantity,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Product ID must not be null");
        this.merchantId = Objects.requireNonNull(merchantId, "Merchant ID must not be null");
        this.sku = validateNonBlank(sku, "sku");
        this.title = validateNonBlank(title, "title");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
        this.costPrice = Objects.requireNonNull(costPrice, "costPrice must not be null");
        this.minMarginPercent = validatePercentage(minMarginPercent, "minMarginPercent");
        this.maxDiscountPercent = validatePercentage(maxDiscountPercent, "maxDiscountPercent");
        if (stockQuantity < 0) {
            throw new DomainException("Stock quantity cannot be negative: " + stockQuantity);
        }
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Product createNew(
            UUID merchantId,
            String sku,
            String title,
            String description,
            Money basePrice,
            Money costPrice,
            BigDecimal minMarginPercent,
            BigDecimal maxDiscountPercent,
            int stockQuantity
    ) {
        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                merchantId,
                sku,
                title,
                description,
                basePrice,
                costPrice,
                minMarginPercent,
                maxDiscountPercent,
                stockQuantity,
                true,
                now,
                now
        );
    }

    /**
     * Computes the deterministic absolute price floor below which no agentic discount may go:
     * floor = costPrice + (costPrice * minMarginPercent / 100)
     */
    public Money calculateFloorPrice() {
        Money marginAmount = costPrice.applyPercentage(minMarginPercent, RoundingMode.CEILING);
        return costPrice.plus(marginAmount);
    }

    /**
     * Deterministically validates whether an offered unit price satisfies the merchant margin floor.
     * Throws MarginFloorViolationException if breached.
     */
    public void validateOfferedPrice(Money offeredUnitPrice) {
        Objects.requireNonNull(offeredUnitPrice, "Offered unit price must not be null");
        Money floorPrice = calculateFloorPrice();
        if (offeredUnitPrice.isLessThan(floorPrice)) {
            throw new MarginFloorViolationException(
                    this.sku,
                    offeredUnitPrice.getAmountInPaise(),
                    floorPrice.getAmountInPaise()
            );
        }
    }

    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Deduction quantity must be positive: " + quantity);
        }
        if (this.stockQuantity < quantity) {
            throw new InventoryExhaustedException(this.sku, quantity, this.stockQuantity);
        }
        this.stockQuantity -= quantity;
        this.updatedAt = Instant.now();
    }

    public void replenishStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Replenishment quantity must be positive: " + quantity);
        }
        this.stockQuantity = Math.addExact(this.stockQuantity, quantity);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public Money getCostPrice() {
        return costPrice;
    }

    public BigDecimal getMinMarginPercent() {
        return minMarginPercent;
    }

    public BigDecimal getMaxDiscountPercent() {
        return maxDiscountPercent;
    }

    public int getStockQuantity() {
        return stockQuantity;
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

    private static String validateNonBlank(String val, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new DomainException("Product field '" + fieldName + "' must not be blank.");
        }
        return val.trim();
    }

    private static BigDecimal validatePercentage(BigDecimal val, String fieldName) {
        Objects.requireNonNull(val, fieldName + " must not be null");
        if (val.compareTo(BigDecimal.ZERO) < 0 || val.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DomainException(fieldName + " must be between 0.00 and 100.00. Given: " + val);
        }
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
