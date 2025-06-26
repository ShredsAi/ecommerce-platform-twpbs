package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Value object representing a Customer ID.
 */
public class DomainCustomerIdValue {
    private final String value;

    public DomainCustomerIdValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CustomerId value cannot be null or blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCustomerIdValue that = (DomainCustomerIdValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}