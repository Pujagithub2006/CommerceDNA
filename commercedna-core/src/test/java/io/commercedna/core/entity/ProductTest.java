package io.commercedna.core.entity;

import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("Should correctly calculate floor price based on cost price and min margin")
    void testFloorPriceCalculation() {
        Product p = Product.createNew(
                UUID.randomUUID(),
                "AURORA-ANC-01",
                "Aurora Headphones",
                "High-end ANC headphones",
                Money.ofPaise(499900L), // Base price: ₹4,999.00
                Money.ofPaise(400000L), // Cost price: ₹4,000.00
                new BigDecimal("15.00"), // Min margin: 15%
                new BigDecimal("20.00"),
                50
        );

        // Floor price = 400000 + (400000 * 15 / 100) = 460000 paise (₹4,600.00)
        Money floorPrice = p.calculateFloorPrice();
        assertThat(floorPrice.getAmountInPaise()).isEqualTo(460000L);
    }

    @Test
    @DisplayName("Should pass offered price at or above margin floor and reject price below floor")
    void testValidateOfferedPrice() {
        Product p = Product.createNew(
                UUID.randomUUID(),
                "AURORA-ANC-01",
                "Aurora Headphones",
                "High-end ANC headphones",
                Money.ofPaise(499900L),
                Money.ofPaise(400000L),
                new BigDecimal("15.00"), // Floor = 460000 paise
                new BigDecimal("20.00"),
                50
        );

        // Price at floor (₹4,600.00) passes
        assertThatCode(() -> p.validateOfferedPrice(Money.ofPaise(460000L)))
                .doesNotThrowAnyException();

        // Price above floor (₹4,700.00) passes
        assertThatCode(() -> p.validateOfferedPrice(Money.ofPaise(470000L)))
                .doesNotThrowAnyException();

        // Price below floor (₹4,599.99 = 459999 paise) is rejected
        assertThatThrownBy(() -> p.validateOfferedPrice(Money.ofPaise(459999L)))
                .isInstanceOf(MarginFloorViolationException.class)
                .hasMessageContaining("below the allowable floor");
    }

    @Test
    @DisplayName("Should deduct stock correctly and reject oversell")
    void testStockManagement() {
        Product p = Product.createNew(
                UUID.randomUUID(),
                "AURORA-ANC-01",
                "Aurora Headphones",
                "High-end ANC headphones",
                Money.ofPaise(499900L),
                Money.ofPaise(400000L),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                10
        );

        p.deductStock(4);
        assertThat(p.getStockQuantity()).isEqualTo(6);

        assertThatThrownBy(() -> p.deductStock(7))
                .isInstanceOf(InventoryExhaustedException.class)
                .hasMessageContaining("Insufficient inventory");
    }
}
