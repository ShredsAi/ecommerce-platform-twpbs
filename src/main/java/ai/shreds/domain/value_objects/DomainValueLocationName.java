package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueLocationName {
    private final String value;

    public DomainValueLocationName(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Location name cannot be null or empty");
        }
        if (value.length() > 128) {
            throw new DomainExceptionInvariantViolation("Location name cannot exceed 128 characters");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueLocationName)) return false;
        DomainValueLocationName that = (DomainValueLocationName) o;
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
