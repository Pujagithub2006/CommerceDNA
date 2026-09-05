package io.commercedna.catalog.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_merchant_sku", columnNames = {"merchant_id", "sku"})
        },
        indexes = {
                @Index(name = "idx_products_merchant_active", columnList = "merchant_id, active"),
                @Index(name = "idx_products_category", columnList = "category"),
                @Index(name = "idx_products_sku", columnList = "sku")
        }
)
public class ProductEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "base_price_paise", nullable = false)
    private Long basePricePaise;

    @Column(name = "cost_price_paise", nullable = false)
    private Long costPricePaise;

    @Column(name = "min_margin_percentage", nullable = false)
    private Double minMarginPercentage;

    @Column(name = "max_discount_percentage", nullable = false)
    private Double maxDiscountPercentage;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    @Column(name = "max_quantity", nullable = false)
    private Integer maxQuantity = 50;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "tags", length = 512)
    private String tags;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProductEntity() {
    }

    public ProductEntity(
            UUID id,
            UUID merchantId,
            String sku,
            String title,
            String description,
            String category,
            String currency,
            Long basePricePaise,
            Long costPricePaise,
            Double minMarginPercentage,
            Double maxDiscountPercentage,
            Integer minQuantity,
            Integer maxQuantity,
            Integer stockQuantity,
            String tags,
            Boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.merchantId = merchantId;
        this.sku = sku;
        this.title = title;
        this.description = description;
        this.category = category;
        this.currency = currency;
        this.basePricePaise = basePricePaise;
        this.costPricePaise = costPricePaise;
        this.minMarginPercentage = minMarginPercentage;
        this.maxDiscountPercentage = maxDiscountPercentage;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.stockQuantity = stockQuantity;
        this.tags = tags;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getBasePricePaise() {
        return basePricePaise;
    }

    public void setBasePricePaise(Long basePricePaise) {
        this.basePricePaise = basePricePaise;
    }

    public Long getCostPricePaise() {
        return costPricePaise;
    }

    public void setCostPricePaise(Long costPricePaise) {
        this.costPricePaise = costPricePaise;
    }

    public Double getMinMarginPercentage() {
        return minMarginPercentage;
    }

    public void setMinMarginPercentage(Double minMarginPercentage) {
        this.minMarginPercentage = minMarginPercentage;
    }

    public Double getMaxDiscountPercentage() {
        return maxDiscountPercentage;
    }

    public void setMaxDiscountPercentage(Double maxDiscountPercentage) {
        this.maxDiscountPercentage = maxDiscountPercentage;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
