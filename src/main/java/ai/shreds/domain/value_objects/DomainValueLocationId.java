package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueLocationId {
    private final String value;

    public DomainValueLocationId(String value) {
        validate(value);
        this.value = value.toUpperCase();
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Location ID cannot be null or empty");
        }
        if (value.length() > 30) {
            throw new DomainExceptionInvariantViolation("Location ID cannot exceed 30 characters");
        }
        if (!value.matches("^[a-zA-Z0-9]+$")) {
            throw new DomainExceptionInvariantViolation("Location ID must be alphanumeric");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueLocationId)) return false;
        DomainValueLocationId that = (DomainValueLocationId) o;
        return value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}