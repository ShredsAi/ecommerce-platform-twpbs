package ai.shreds.shared.value_objects;

import lombok.Value;
import lombok.Builder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing an amount of money with currency.
 */
@Value
@Builder
public class SharedMoneyValue {

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.00", inclusive = true, message = "Amount must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Amount can have up to 2 decimal places and 12 integer digits")
    BigDecimal value;

    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    String currency;

    /**
     * Factory method to create a SharedMoneyValue instance with validation.
     *
     * @param value    the monetary amount
     * @param currency the ISO 4217 currency code
     * @return a new SharedMoneyValue instance
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static SharedMoneyValue of(BigDecimal value, String currency) {
        Objects.requireNonNull(value, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency must be a valid ISO 4217 code");
        }
        return new SharedMoneyValue(value, currency);
    }

    /**
     * Add another money value (must be same currency).
     *
     * @param other the other money value to add
     * @return a new SharedMoneyValue with the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public SharedMoneyValue add(SharedMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new SharedMoneyValue(this.value.add(other.value), this.currency);
    }

    /**
     * Multiply by a factor.
     *
     * @param factor the multiplication factor
     * @return a new SharedMoneyValue with the multiplied amount
     */
    public SharedMoneyValue multiply(BigDecimal factor) {
        return new SharedMoneyValue(this.value.multiply(factor), this.currency);
    }
}