package io.commercedna.catalog.service;

import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.repository.ProductJpaRepository;
import io.commercedna.core.entity.Product;
import io.commercedna.core.exception.DomainException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.core.model.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {

    private final ProductJpaRepository productRepository;

    public CatalogService(ProductJpaRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public record CreateProductCommand(
            UUID merchantId,
            String sku,
            String title,
            String description,
            String category,
            String currency,
            long basePricePaise,
            long costPricePaise,
            double minMarginPercentage,
            double maxDiscountPercentage,
            int minQuantity,
            int maxQuantity,
            int stockQuantity,
            String tags
    ) {}

    public ProductEntity createProduct(CreateProductCommand cmd) {
        Objects.requireNonNull(cmd, "CreateProductCommand must not be null");
        Objects.requireNonNull(cmd.merchantId(), "merchantId must not be null");

        String normalizedSku = cmd.sku().trim().toUpperCase();

        if (productRepository.existsByMerchantIdAndSku(cmd.merchantId(), normalizedSku)) {
            throw new DomainException("Product with SKU '" + normalizedSku + "' already exists for this merchant.");
        }

        if (cmd.basePricePaise() <= 0) {
            throw new DomainException("Base price must be greater than zero paise.");
        }

        if (cmd.costPricePaise() < 0) {
            throw new DomainException("Cost price cannot be negative.");
        }

        if (cmd.basePricePaise() < cmd.costPricePaise()) {
            throw new DomainException("Base price cannot be less than cost price.");
        }

        if (cmd.minQuantity() <= 0 || cmd.maxQuantity() < cmd.minQuantity()) {
            throw new DomainException("Invalid quantity limits: minQuantity must be > 0 and maxQuantity >= minQuantity.");
        }

        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        ProductEntity entity = new ProductEntity(
                productId,
                cmd.merchantId(),
                normalizedSku,
                cmd.title().trim(),
                cmd.description() != null ? cmd.description().trim() : "",
                cmd.category().trim(),
                cmd.currency() != null ? cmd.currency().trim().toUpperCase() : "INR",
                cmd.basePricePaise(),
                cmd.costPricePaise(),
                cmd.minMarginPercentage(),
                cmd.maxDiscountPercentage(),
                cmd.minQuantity(),
                cmd.maxQuantity(),
                cmd.stockQuantity(),
                cmd.tags() != null ? cmd.tags().trim() : "",
                true,
                now,
                now
        );

        return productRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> getMerchantProducts(UUID merchantId) {
        return productRepository.findByMerchantIdAndActiveTrue(merchantId);
    }

    @Transactional(readOnly = true)
    public ProductEntity getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
    }

    @Transactional(readOnly = true)
    public ProductEntity getProductByMerchantAndSku(UUID merchantId, String sku) {
        return productRepository.findByMerchantIdAndSku(merchantId, sku.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU", sku));
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> searchProducts(UUID merchantId, String query) {
        String sanitizedQuery = query != null ? query.trim() : "";
        if (merchantId != null) {
            return productRepository.searchMerchantProducts(merchantId, sanitizedQuery);
        } else {
            return productRepository.searchActiveProducts(sanitizedQuery);
        }
    }

    public synchronized void decrementStock(UUID productId, int quantity) {
        ProductEntity entity = getProductById(productId);
        if (entity.getStockQuantity() < quantity) {
            throw new InventoryExhaustedException(entity.getSku(), quantity, entity.getStockQuantity());
        }
        entity.setStockQuantity(entity.getStockQuantity() - quantity);
        entity.setUpdatedAt(Instant.now());
        productRepository.save(entity);
    }

    public Product toDomainProduct(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getMerchantId(),
                entity.getSku(),
                entity.getTitle(),
                entity.getDescription(),
                Money.ofPaise(entity.getBasePricePaise()),
                Money.ofPaise(entity.getCostPricePaise()),
                java.math.BigDecimal.valueOf(entity.getMinMarginPercentage() * 100.0),
                java.math.BigDecimal.valueOf(entity.getMaxDiscountPercentage() * 100.0),
                entity.getStockQuantity(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
