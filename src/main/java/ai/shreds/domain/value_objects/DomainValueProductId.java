package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueProductId {
    private final String value;

    public DomainValueProductId(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Product ID cannot be null or empty");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueProductId)) return false;
        DomainValueProductId that = (DomainValueProductId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}