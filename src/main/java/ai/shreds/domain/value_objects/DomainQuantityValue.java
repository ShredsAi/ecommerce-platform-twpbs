package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Value object representing a quantity with validation rules.
 */
public class DomainQuantityValue {
    private final Integer value;

    public DomainQuantityValue(Integer value) {
        this.value = value;
        validate();
    }

    public Integer getValue() {
        return value;
    }

    /**
     * Validates that the quantity is greater than 0 and at most 999.
     */
    public void validate() {
        if (value == null) {
            throw new IllegalArgumentException("Quantity value cannot be null");
        }
        if (value <= 0 || value > 999) {
            throw new IllegalArgumentException("Quantity must be greater than 0 and maximum 999");
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainQuantityValue that = (DomainQuantityValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}