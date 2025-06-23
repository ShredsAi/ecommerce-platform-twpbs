package ai.shreds.domain.value_objects;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a monetary amount and currency in the domain.
 */
public final class DomainMoneyValue {
    private final BigDecimal amount;
    private final String currency;

    public DomainMoneyValue(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency.trim().length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid ISO-4217 code");
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public DomainMoneyValue add(DomainMoneyValue other) {
        validateCurrencyMatch(other);
        return new DomainMoneyValue(this.amount.add(other.amount), this.currency);
    }

    public DomainMoneyValue subtract(DomainMoneyValue other) {
        validateCurrencyMatch(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative");
        }
        return new DomainMoneyValue(result, this.currency);
    }

    public DomainMoneyValue multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor cannot be null");
        return new DomainMoneyValue(this.amount.multiply(factor), this.currency);
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    public boolean isPositive() {
        return this.amount.signum() > 0;
    }

    private void validateCurrencyMatch(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    public SharedMoneyValue toSharedValue() {
        return SharedMoneyValue.fromDomainValue(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainMoneyValue)) return false;
        DomainMoneyValue that = (DomainMoneyValue) o;
        return amount.equals(that.amount) && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %s", currency, amount);
    }
}