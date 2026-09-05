package io.commercedna.catalog.engine;

import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.exception.QuantityQuotaExceededException;
import io.commercedna.core.model.Money;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Deterministic Merchant Margin Guardrail Engine.
 * Enforces mathematically unbreakable profit floors and quota constraints.
 * Operates purely in 64-bit integer paise.
 */
@Component
public class MarginGuardrailEngine {

    public record EvaluationResult(
            boolean approved,
            Money minimumAcceptableUnitPrice,
            Money proposedUnitPrice,
            Money totalApprovedAmount,
            double appliedDiscountPercentage,
            String rationale
    ) {}

    /**
     * Evaluates whether a proposed unit price and quantity strictly satisfy merchant profit margin floors.
     *
     * @param product The merchant product entity containing cost, base price, and margin rules.
     * @param proposedUnitPrice The unit price offered by the Buyer Agent.
     * @param quantity The requested quantity.
     * @return EvaluationResult containing the computed floor and approval rationale.
     * @throws MarginFloorViolationException if proposed price is below the unbreakable floor.
     * @throws QuantityQuotaExceededException if quantity exceeds merchant limits.
     */
    public EvaluationResult evaluateProposal(ProductEntity product, Money proposedUnitPrice, int quantity) {
        Objects.requireNonNull(product, "Product must not be null");
        Objects.requireNonNull(proposedUnitPrice, "Proposed unit price must not be null");

        // 1. Validate Quantity Quotas
        if (quantity < product.getMinQuantity()) {
            throw new QuantityQuotaExceededException(
                    "Proposed quantity " + quantity + " is below the minimum order quantity of " + product.getMinQuantity()
            );
        }
        if (quantity > product.getMaxQuantity()) {
            throw new QuantityQuotaExceededException(
                    product.getSku(),
                    quantity,
                    product.getMaxQuantity()
            );
        }

        // 2. Compute Cost Margin Floor (paise): costPrice * (1 + minMargin)
        // Using integer math with ceiling to guarantee the merchant never loses even a fraction of a paisa:
        // cost_floor_paise = ceil(costPricePaise * (10000 + minMarginBasisPoints) / 10000)
        long minMarginBasisPoints = Math.round(product.getMinMarginPercentage() * 10000.0);
        long costFloorPaise = divideCeil(product.getCostPricePaise() * (10000 + minMarginBasisPoints), 10000);

        // 3. Compute Max Discount Floor (paise): basePrice * (1 - maxDiscount)
        long maxDiscountBasisPoints = Math.round(product.getMaxDiscountPercentage() * 10000.0);
        long maxDiscountFloorPaise = divideCeil(product.getBasePricePaise() * (10000 - maxDiscountBasisPoints), 10000);

        // Standard floor is the maximum of cost-plus floor and max-discount floor
        long effectiveFloorPaise = Math.max(costFloorPaise, maxDiscountFloorPaise);

        // 4. Volume Incentive Tier:
        // If order quantity >= 10, provide up to an additional 2.5% volume discount,
        // BUT NEVER allow the price to fall below the strict cost floor!
        if (quantity >= 10) {
            long volumeDiscountAllowance = divideCeil(product.getBasePricePaise() * 250, 10000); // 2.5%
            long volumeAdjustedFloor = Math.max(costFloorPaise, effectiveFloorPaise - volumeDiscountAllowance);
            effectiveFloorPaise = volumeAdjustedFloor;
        }

        Money minimumAcceptableUnitPrice = Money.ofPaise(effectiveFloorPaise);

        // 5. Enforce Unbreakable Invariant
        if (proposedUnitPrice.isLessThan(minimumAcceptableUnitPrice)) {
            throw new MarginFloorViolationException(
                    product.getSku(),
                    proposedUnitPrice.getAmountInPaise(),
                    minimumAcceptableUnitPrice.getAmountInPaise()
            );
        }

        // 6. Compute Applied Discount Percentage relative to base price
        long basePricePaise = product.getBasePricePaise();
        double discountPct = 0.0;
        if (basePricePaise > 0 && proposedUnitPrice.getAmountInPaise() < basePricePaise) {
            discountPct = ((double) (basePricePaise - proposedUnitPrice.getAmountInPaise()) / (double) basePricePaise) * 100.0;
        }

        Money totalAmount = proposedUnitPrice.multiply(quantity);

        return new EvaluationResult(
                true,
                minimumAcceptableUnitPrice,
                proposedUnitPrice,
                totalAmount,
                Math.round(discountPct * 100.0) / 100.0,
                "Proposal satisfies deterministic merchant margin floor of " + minimumAcceptableUnitPrice.formatInRupees()
        );
    }

    private static long divideCeil(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
