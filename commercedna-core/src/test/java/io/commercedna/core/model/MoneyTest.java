package io.commercedna.core.model;

import io.commercedna.core.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("Should create Money in paise and format in rupees")
    void testCreationAndFormatting() {
        Money price = Money.ofPaise(499900L, Currency.INR);
        assertThat(price.getAmountInPaise()).isEqualTo(499900L);
        assertThat(price.getCurrency()).isEqualTo(Currency.INR);
        assertThat(price.toRupees()).isEqualByComparingTo(new BigDecimal("4999.00"));
        assertThat(price.formatInRupees()).isEqualTo("INR 4999.00");
    }

    @Test
    @DisplayName("Should create Money using rupee helper")
    void testOfRupees() {
        Money m = Money.ofRupees(150L);
        assertThat(m.getAmountInPaise()).isEqualTo(15000L);
        assertThat(m.formatInRupees()).isEqualTo("INR 150.00");
    }

    @Test
    @DisplayName("Should reject negative monetary values")
    void testNegativeRejected() {
        assertThatThrownBy(() -> Money.ofPaise(-100L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Monetary value cannot be negative");
    }

    @Test
    @DisplayName("Should perform exact integer addition without precision loss")
    void testAddition() {
        Money m1 = Money.ofPaise(10050L);
        Money m2 = Money.ofPaise(20025L);
        Money sum = m1.plus(m2);

        assertThat(sum.getAmountInPaise()).isEqualTo(30075L);
    }

    @Test
    @DisplayName("Should perform subtraction and reject negative results")
    void testSubtraction() {
        Money m1 = Money.ofPaise(50000L);
        Money m2 = Money.ofPaise(12500L);
        Money diff = m1.minus(m2);

        assertThat(diff.getAmountInPaise()).isEqualTo(37500L);

        assertThatThrownBy(() -> m2.minus(m1))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Subtraction results in negative monetary balance");
    }

    @Test
    @DisplayName("Should multiply integer quantities accurately")
    void testMultiplication() {
        Money unitPrice = Money.ofPaise(450000L); // ₹4,500.00
        Money total = unitPrice.multiply(5);

        assertThat(total.getAmountInPaise()).isEqualTo(2250000L); // ₹22,500.00
    }

    @Test
    @DisplayName("Should calculate percentages with explicit rounding mode")
    void testApplyPercentage() {
        Money cost = Money.ofPaise(400000L); // ₹4,000.00
        BigDecimal marginPercent = new BigDecimal("15.50"); // 15.5%
        Money margin = cost.applyPercentage(marginPercent, RoundingMode.CEILING);

        // 400000 * 15.5 / 100 = 62000 paise
        assertThat(margin.getAmountInPaise()).isEqualTo(62000L);
    }

    @Test
    @DisplayName("Should correctly evaluate relational comparisons")
    void testComparisons() {
        Money low = Money.ofPaise(1000L);
        Money high = Money.ofPaise(2000L);

        assertThat(high.isGreaterThanOrEqual(low)).isTrue();
        assertThat(low.isLessThan(high)).isTrue();
        assertThat(low.isGreaterThanOrEqual(low)).isTrue();
    }
}
