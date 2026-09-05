package io.commercedna.api.controller;

import io.commercedna.api.dto.CreateProductRequest;
import io.commercedna.api.dto.ProductResponse;
import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.service.CatalogService;
import io.commercedna.core.entity.Merchant;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.identity.service.MerchantIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@Tag(name = "Product Catalog & Discovery", description = "Merchant catalog management, semantic search, and agentic product discovery")
public class CatalogController {

    private final CatalogService catalogService;
    private final MerchantIdentityService merchantIdentityService;

    public CatalogController(CatalogService catalogService, MerchantIdentityService merchantIdentityService) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.merchantIdentityService = Objects.requireNonNull(merchantIdentityService);
    }

    @PostMapping("/api/v1/merchants/{merchantId}/products")
    @Operation(summary = "Add Product to Merchant Catalog", description = "Adds a new product to the merchant catalog with deterministic margin floor rules")
    public ResponseEntity<ProductResponse> addProduct(
            @PathVariable UUID merchantId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        // Validate merchant exists
        merchantIdentityService.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        long basePaise = request.basePriceRupees().multiply(BigDecimal.valueOf(100)).longValueExact();
        long costPaise = request.costPriceRupees().multiply(BigDecimal.valueOf(100)).longValueExact();

        CatalogService.CreateProductCommand cmd = new CatalogService.CreateProductCommand(
                merchantId,
                request.sku(),
                request.title(),
                request.description(),
                request.category(),
                request.currency() != null ? request.currency() : "INR",
                basePaise,
                costPaise,
                request.minMarginPercentage(),
                request.maxDiscountPercentage(),
                request.minQuantity() != null ? request.minQuantity() : 1,
                request.maxQuantity() != null ? request.maxQuantity() : 50,
                request.stockQuantity() != null ? request.stockQuantity() : 0,
                request.tags() != null ? request.tags() : ""
        );

        ProductEntity saved = catalogService.createProduct(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(saved));
    }

    @GetMapping("/api/v1/merchants/{merchantId}/products")
    @Operation(summary = "List Merchant Products", description = "Retrieves all active products belonging to the merchant")
    public ResponseEntity<List<ProductResponse>> listMerchantProducts(@PathVariable UUID merchantId) {
        List<ProductResponse> products = catalogService.getMerchantProducts(merchantId).stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/api/v1/merchants/{merchantId}/products/{productId}")
    @Operation(summary = "Get Product Details", description = "Retrieves details of a specific product")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID merchantId,
            @PathVariable UUID productId
    ) {
        ProductEntity entity = catalogService.getProductById(productId);
        if (!entity.getMerchantId().equals(merchantId)) {
            throw new ResourceNotFoundException("Product", productId.toString());
        }
        return ResponseEntity.ok(ProductResponse.fromEntity(entity));
    }

    @GetMapping("/api/v1/catalog/search")
    @Operation(summary = "Agentic Catalog Search", description = "Allows autonomous Buyer Agents to discover products by semantic keywords or merchant")
    public ResponseEntity<List<ProductResponse>> searchCatalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String merchantCode
    ) {
        UUID merchantId = null;
        if (merchantCode != null && !merchantCode.isBlank()) {
            Merchant merchant = merchantIdentityService.findByMerchantCode(merchantCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant with code", merchantCode));
            merchantId = merchant.getId();
        }

        List<ProductResponse> results = catalogService.searchProducts(merchantId, q).stream()
                .map(ProductResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(results);
    }
}
