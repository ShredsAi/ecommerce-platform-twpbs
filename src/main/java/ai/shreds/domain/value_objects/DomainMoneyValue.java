package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainValidationException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Getter
public class DomainMoneyValue {
    private final BigDecimal amount;
    private final String currency;

    public DomainMoneyValue(BigDecimal amount, String currency) {
        validate(amount, currency);
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }

    public void validate() {
        validate(this.amount, this.currency);
    }

    private void validate(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new DomainValidationException("Amount cannot be null", 
                List.of("amount must be provided"));
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Amount cannot be negative", 
                List.of("amount must be >= 0"));
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new DomainValidationException("Currency cannot be null or empty", 
                List.of("currency must be provided"));
        }
        if (currency.length() != 3) {
            throw new DomainValidationException("Currency must be a valid 3-character ISO code", 
                List.of("currency must be a 3-character ISO-4217 code"));
        }
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new DomainValidationException("Currency must contain only uppercase letters", 
                List.of("currency must be in uppercase format"));
        }
    }

    public DomainMoneyValue add(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainValidationException("Cannot add money with different currencies", 
                List.of("Both amounts must have the same currency"));
        }
        return new DomainMoneyValue(this.amount.add(other.amount), this.currency);
    }

    public DomainMoneyValue multiply(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new DomainValidationException("Quantity must be non-negative", 
                List.of("quantity must be >= 0"));
        }
        return new DomainMoneyValue(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public DomainMoneyValue subtract(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainValidationException("Cannot subtract money with different currencies", 
                List.of("Both amounts must have the same currency"));
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Result cannot be negative", 
                List.of("Subtraction result must be >= 0"));
        }
        return new DomainMoneyValue(result, this.currency);
    }

    public boolean isGreaterThan(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainValidationException("Cannot compare money with different currencies", 
                List.of("Both amounts must have the same currency"));
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainMoneyValue that = (DomainMoneyValue) o;
        return Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}