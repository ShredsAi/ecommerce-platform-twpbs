package ai.shreds.shared.value_objects;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable representation of monetary values with currency.
 * Provides operations for common money manipulations with currency validation.
 */
@Data
@NoArgsConstructor // Required for JPA/serialization
@AllArgsConstructor
@Embeddable
public class SharedValueMoney {
    
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Creates a money value with validation.
     * 
     * @param amount The monetary amount (must not be null)
     * @param currency ISO 4217 currency code (must not be null or empty)
     * @return A new validated SharedValueMoney instance
     * @throws IllegalArgumentException if amount or currency is invalid
     */
    public static SharedValueMoney of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        if (currency.trim().isEmpty() || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter ISO code");
        }
        return new SharedValueMoney(amount.setScale(2, RoundingMode.HALF_UP), currency.toUpperCase());
    }

    /**
     * Creates a zero money value for the specified currency.
     * 
     * @param currency ISO 4217 currency code
     * @return A new SharedValueMoney with zero amount in the specified currency
     */
    public static SharedValueMoney zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }

    /**
     * Adds another money value to this one.
     * 
     * @param other The money value to add (must be in the same currency)
     * @return A new SharedValueMoney representing the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public SharedValueMoney add(SharedValueMoney other) {
        validateCurrency(other);
        return new SharedValueMoney(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another money value from this one.
     * 
     * @param other The money value to subtract (must be in the same currency)
     * @return A new SharedValueMoney representing the difference
     * @throws IllegalArgumentException if currencies don't match
     */
    public SharedValueMoney subtract(SharedValueMoney other) {
        validateCurrency(other);
        return new SharedValueMoney(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this money value by a factor.
     * 
     * @param factor The multiplication factor
     * @return A new SharedValueMoney representing the product
     */
    public SharedValueMoney multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplication factor must not be null");
        return new SharedValueMoney(this.amount.multiply(factor).setScale(2, RoundingMode.HALF_UP), this.currency);
    }

    /**
     * Checks if this money value is positive (greater than zero).
     * 
     * @return true if amount > 0, false otherwise
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this money value is negative (less than zero).
     * 
     * @return true if amount < 0, false otherwise
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if this money value is zero.
     * 
     * @return true if amount = 0, false otherwise
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Compares this money value against another one for equality of amount and currency.
     * 
     * @param other The money value to compare with
     * @return true if both amount and currency are equal, false otherwise
     */
    public boolean equals(SharedValueMoney other) {
        if (other == null) {
            return false;
        }
        return this.currency.equals(other.currency) && 
               this.amount.compareTo(other.amount) == 0;
    }

    /**
     * Validates that the provided money object has the same currency as this one.
     * 
     * @param other The money value to validate
     * @throws IllegalArgumentException if currencies don't match
     */
    private void validateCurrency(SharedValueMoney other) {
        if (other == null || !this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + 
                (other == null ? "null" : other.currency));
        }
    }
    
    /**
     * Returns a string representation of this money value,
     * e.g. "USD 10.50"
     * 
     * @return A string in the format "CURRENCY AMOUNT"
     */
    @Override
    public String toString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }
}