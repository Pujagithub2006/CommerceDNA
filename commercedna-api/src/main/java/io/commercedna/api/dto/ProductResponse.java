package io.commercedna.api.dto;

import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.core.model.Money;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID merchantId,
        String sku,
        String title,
        String description,
        String category,
        String currency,
        Long basePricePaise,
        String basePriceFormatted,
        Integer minQuantity,
        Integer maxQuantity,
        Integer stockQuantity,
        Boolean inStock,
        String tags,
        Boolean active,
        Instant createdAt
) {
    public static ProductResponse fromEntity(ProductEntity entity) {
        Money money = Money.ofPaise(entity.getBasePricePaise());
        return new ProductResponse(
                entity.getId(),
                entity.getMerchantId(),
                entity.getSku(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getCurrency(),
                entity.getBasePricePaise(),
                money.formatInRupees(),
                entity.getMinQuantity(),
                entity.getMaxQuantity(),
                entity.getStockQuantity(),
                entity.getStockQuantity() > 0,
                entity.getTags(),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }
}
