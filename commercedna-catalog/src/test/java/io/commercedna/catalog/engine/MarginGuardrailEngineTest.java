package io.commercedna.catalog.engine;

import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.exception.QuantityQuotaExceededException;
import io.commercedna.core.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarginGuardrailEngineTest {

    private MarginGuardrailEngine engine;
    private ProductEntity sampleProduct;

    @BeforeEach
    void setUp() {
        engine = new MarginGuardrailEngine();

        // Base Price: 5000.00 INR (500,000 paise)
        // Cost Price: 3500.00 INR (350,000 paise)
        // Min Margin: 15% -> Cost floor = 350,000 * 1.15 = 402,500 paise (4025.00 INR)
        // Max Discount: 20% -> Discount floor = 500,000 * 0.80 = 400,000 paise (4000.00 INR)
        // Standard minimum acceptable floor = max(402,500, 400,000) = 402,500 paise (4025.00 INR)
        sampleProduct = new ProductEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TITAN-OCTANE-01",
                "Titan Octane Chronograph Watch",
                "High precision chronograph watch with sapphire crystal",
                "Watches",
                "INR",
                500000L, // 5000.00 INR
                350000L, // 3500.00 INR
                0.15,    // 15% min margin
                0.20,    // 20% max discount
                1,       // min qty
                25,      // max qty
                50,      // stock
                "watches,chronograph,titan",
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should approve proposal at exact base price with 0% discount")
    void shouldApproveProposalAtBasePrice() {
        Money proposedPrice = Money.ofPaise(500000L); // 5000.00 INR
        MarginGuardrailEngine.EvaluationResult result = engine.evaluateProposal(sampleProduct, proposedPrice, 1);

        assertThat(result.approved()).isTrue();
        assertThat(result.appliedDiscountPercentage()).isEqualTo(0.0);
        assertThat(result.totalApprovedAmount()).isEqualTo(Money.ofPaise(500000L));
    }

    @Test
    @DisplayName("Should approve proposal within acceptable margin floor (4500.00 INR)")
    void shouldApproveProposalWithinMarginFloor() {
        Money proposedPrice = Money.ofPaise(450000L); // 4500.00 INR (10% discount)
        MarginGuardrailEngine.EvaluationResult result = engine.evaluateProposal(sampleProduct, proposedPrice, 2);

        assertThat(result.approved()).isTrue();
        assertThat(result.appliedDiscountPercentage()).isEqualTo(10.0);
        assertThat(result.totalApprovedAmount()).isEqualTo(Money.ofPaise(900000L));
    }

    @Test
    @DisplayName("Should reject proposal below minimum acceptable floor (3900.00 INR vs 4025.00 INR floor)")
    void shouldRejectProposalBelowMarginFloor() {
        Money proposedPrice = Money.ofPaise(390000L); // 3900.00 INR

        assertThatThrownBy(() -> engine.evaluateProposal(sampleProduct, proposedPrice, 1))
                .isInstanceOf(MarginFloorViolationException.class)
                .hasMessageContaining("below the allowable floor");
    }

    @Test
    @DisplayName("Should apply volume incentive tier discount for quantity >= 10 without breaching cost floor")
    void shouldApplyVolumeIncentiveForLargeOrders() {
        // Floor without volume incentive = 402,500 paise
        // Cost floor is 402,500 paise. So volume discount will never breach 402,500.
        Money proposedPrice = Money.ofPaise(405000L); // 4050.00 INR

        MarginGuardrailEngine.EvaluationResult result = engine.evaluateProposal(sampleProduct, proposedPrice, 12);

        assertThat(result.approved()).isTrue();
        assertThat(result.totalApprovedAmount()).isEqualTo(Money.ofPaise(405000L * 12));
    }

    @Test
    @DisplayName("Should reject proposal when quantity is below minimum order quantity")
    void shouldRejectWhenQuantityBelowMin() {
        sampleProduct.setMinQuantity(5);
        Money price = Money.ofPaise(500000L);

        assertThatThrownBy(() -> engine.evaluateProposal(sampleProduct, price, 3))
                .isInstanceOf(QuantityQuotaExceededException.class)
                .hasMessageContaining("below the minimum order quantity");
    }

    @Test
    @DisplayName("Should reject proposal when quantity exceeds maximum purchase quota")
    void shouldRejectWhenQuantityExceedsMax() {
        sampleProduct.setMaxQuantity(10);
        Money price = Money.ofPaise(500000L);

        assertThatThrownBy(() -> engine.evaluateProposal(sampleProduct, price, 15))
                .isInstanceOf(QuantityQuotaExceededException.class)
                .hasMessageContaining("exceeds maximum allowable single-buyer quota");
    }
}
