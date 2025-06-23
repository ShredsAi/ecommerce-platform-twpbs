package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a Customer identifier.
 */
public final class DomainCustomerIdValue {
    private final UUID value;

    public DomainCustomerIdValue(UUID value) {
        this.value = Objects.requireNonNull(value, "CustomerIdValue cannot be null");
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCustomerIdValue that = (DomainCustomerIdValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}