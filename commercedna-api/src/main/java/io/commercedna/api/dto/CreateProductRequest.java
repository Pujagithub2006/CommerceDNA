package io.commercedna.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "sku is required")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,64}$", message = "sku must be 3-64 alphanumeric characters, dashes, or underscores")
        String sku,

        @NotBlank(message = "title is required")
        String title,

        String description,

        @NotBlank(message = "category is required")
        String category,

        String currency,

        @NotNull(message = "basePriceRupees is required")
        @DecimalMin(value = "0.01", message = "basePriceRupees must be at least 0.01")
        BigDecimal basePriceRupees,

        @NotNull(message = "costPriceRupees is required")
        @DecimalMin(value = "0.00", message = "costPriceRupees cannot be negative")
        BigDecimal costPriceRupees,

        @NotNull(message = "minMarginPercentage is required")
        @DecimalMin(value = "0.00", message = "minMarginPercentage cannot be negative")
        @Max(value = 1, message = "minMarginPercentage must be expressed as a fraction <= 1.0 (e.g. 0.15 for 15%)")
        Double minMarginPercentage,

        @NotNull(message = "maxDiscountPercentage is required")
        @DecimalMin(value = "0.00", message = "maxDiscountPercentage cannot be negative")
        @Max(value = 1, message = "maxDiscountPercentage must be expressed as a fraction <= 1.0 (e.g. 0.20 for 20%)")
        Double maxDiscountPercentage,

        @Min(value = 1, message = "minQuantity must be at least 1")
        Integer minQuantity,

        @Min(value = 1, message = "maxQuantity must be at least 1")
        Integer maxQuantity,

        @Min(value = 0, message = "stockQuantity cannot be negative")
        Integer stockQuantity,

        String tags
) {}
