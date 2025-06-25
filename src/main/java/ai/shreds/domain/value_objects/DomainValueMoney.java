package ai.shreds.domain.value_objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Domain value object representing monetary values.
 * Contains business logic for monetary calculations, currency validation, and arithmetic operations.
 */
public final class DomainValueMoney {
    
    private final BigDecimal amount;
    private final String currency;
    
    // Standard rounding for monetary calculations
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Create a new Money value object.
     *
     * @param amount the monetary amount
     * @param currency the currency code (ISO 4217)
     * @throws IllegalArgumentException if amount or currency is invalid
     */
    public DomainValueMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places");
        }
        try {
            Currency.getInstance(currency);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ISO 4217 currency code", e);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    /**
     * Create a new Money value object from double.
     *
     * @param amount the monetary amount as double
     * @param currency the currency code (ISO 4217)
     * @throws IllegalArgumentException if amount or currency is invalid
     */
    public DomainValueMoney(double amount, String currency) {
        this(BigDecimal.valueOf(amount), currency);
    }
    
    /**
     * Create a zero money value for the given currency.
     */
    public static DomainValueMoney zero(String currency) {
        return new DomainValueMoney(BigDecimal.ZERO, currency);
    }
    
    /**
     * Add another money value to this one.
     * Both values must have the same currency.
     */
    public DomainValueMoney add(DomainValueMoney other) {
        Objects.requireNonNull(other, "Other money value cannot be null");
        
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot add different currencies: %s and %s", this.currency, other.currency)
            );
        }
        
        return new DomainValueMoney(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Subtract another money value from this one.
     * Both values must have the same currency.
     */
    public DomainValueMoney subtract(DomainValueMoney other) {
        Objects.requireNonNull(other, "Other money value cannot be null");
        
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot subtract different currencies: %s and %s", this.currency, other.currency)
            );
        }
        
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative amount");
        }
        
        return new DomainValueMoney(result, this.currency);
    }
    
    /**
     * Multiply this money value by a factor.
     */
    public DomainValueMoney multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor cannot be null");
        
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Factor cannot be negative: " + factor);
        }
        
        return new DomainValueMoney(this.amount.multiply(factor), this.currency);
    }
    
    /**
     * Multiply this money value by a double factor.
     */
    public DomainValueMoney multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }
    
    /**
     * Calculate percentage of this amount.
     */
    public DomainValueMoney percentage(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100: " + percentage);
        }
        
        return multiply(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING_MODE));
    }
    
    /**
     * Check if this money value is zero.
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if this money value is greater than another.
     */
    public boolean isGreaterThan(DomainValueMoney other) {
        Objects.requireNonNull(other, "Other money value cannot be null");
        
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot compare different currencies: %s and %s", this.currency, other.currency)
            );
        }
        
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Check if this money value is greater than or equal to another.
     */
    public boolean isGreaterThanOrEqualTo(DomainValueMoney other) {
        Objects.requireNonNull(other, "Other money value cannot be null");
        
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot compare different currencies: %s and %s", this.currency, other.currency)
            );
        }
        
        return this.amount.compareTo(other.amount) >= 0;
    }
    
    /**
     * Business logic validation.
     */
    public boolean validate() {
        return amount != null && 
               amount.compareTo(BigDecimal.ZERO) >= 0 && 
               currency != null;
    }
    
    // Getters
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    
    // Legacy getters for backward compatibility
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueMoney)) return false;
        DomainValueMoney that = (DomainValueMoney) o;
        return amount.equals(that.amount) && currency.equals(that.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}