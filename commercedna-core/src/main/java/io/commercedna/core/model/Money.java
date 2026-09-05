package io.commercedna.core.model;

import io.commercedna.core.exception.DomainException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing monetary value strictly as 64-bit integer paise.
 * Adheres to CommerceDNA Invariant: Zero floating-point representation in financial accounting.
 */
public final class Money implements Comparable<Money>, Serializable {

    private static final long serialVersionUID = 1L;

    private final long amountInPaise;
    private final Currency currency;

    private Money(long amountInPaise, Currency currency) {
        if (amountInPaise < 0) {
            throw new DomainException("Monetary value cannot be negative: " + amountInPaise + " paise");
        }
        this.amountInPaise = amountInPaise;
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
    }

    public static Money ofPaise(long amountInPaise, Currency currency) {
        return new Money(amountInPaise, currency);
    }

    public static Money ofPaise(long amountInPaise) {
        return new Money(amountInPaise, Currency.INR);
    }

    public static Money ofRupees(long rupees) {
        if (rupees < 0) {
            throw new DomainException("Rupee amount cannot be negative: " + rupees);
        }
        return new Money(Math.multiplyExact(rupees, 100L), Currency.INR);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    public static Money zero() {
        return zero(Currency.INR);
    }

    public long getAmountInPaise() {
        return amountInPaise;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money plus(Money other) {
        validateSameCurrency(other);
        return new Money(Math.addExact(this.amountInPaise, other.amountInPaise), this.currency);
    }

    public Money minus(Money other) {
        validateSameCurrency(other);
        long result = Math.subtractExact(this.amountInPaise, other.amountInPaise);
        if (result < 0) {
            throw new DomainException("Subtraction results in negative monetary balance: " + result);
        }
        return new Money(result, this.currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new DomainException("Multiplier quantity cannot be negative: " + quantity);
        }
        return new Money(Math.multiplyExact(this.amountInPaise, (long) quantity), this.currency);
    }

    public Money applyPercentage(BigDecimal percentage, RoundingMode roundingMode) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        Objects.requireNonNull(roundingMode, "RoundingMode must not be null");
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Percentage cannot be negative: " + percentage);
        }
        BigDecimal paiseDecimal = BigDecimal.valueOf(this.amountInPaise);
        BigDecimal calculated = paiseDecimal
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 0, roundingMode);
        return new Money(calculated.longValueExact(), this.currency);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amountInPaise >= other.amountInPaise;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amountInPaise < other.amountInPaise;
    }

    public boolean isZero() {
        return this.amountInPaise == 0L;
    }

    public BigDecimal toRupees() {
        return BigDecimal.valueOf(this.amountInPaise)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
    }

    public String formatInRupees() {
        return String.format("%s %.2f", currency.name(), toRupees());
    }

    private void validateSameCurrency(Money other) {
        Objects.requireNonNull(other, "Comparison Money object must not be null");
        if (this.currency != other.currency) {
            throw new DomainException(String.format(
                    "Currency mismatch: Cannot operate between %s and %s",
                    this.currency, other.currency
            ));
        }
    }

    @Override
    public int compareTo(Money other) {
        validateSameCurrency(other);
        return Long.compare(this.amountInPaise, other.amountInPaise);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amountInPaise == money.amountInPaise && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountInPaise, currency);
    }

    @Override
    public String toString() {
        return formatInRupees() + " (" + amountInPaise + " paise)";
    }
}
