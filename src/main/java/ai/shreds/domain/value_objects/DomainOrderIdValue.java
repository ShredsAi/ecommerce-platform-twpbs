package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an Order identifier in the domain.
 */
public final class DomainOrderIdValue {

    private final UUID value;

    public DomainOrderIdValue(UUID value) {
        this.value = Objects.requireNonNull(value, "OrderIdValue cannot be null");
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
        DomainOrderIdValue that = (DomainOrderIdValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}