package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueSkuId {
    private final String value;

    public DomainValueSkuId(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("SKU ID cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new DomainExceptionInvariantViolation("SKU ID cannot exceed 50 characters");
        }
        if (!value.matches("^[a-zA-Z0-9\\-]+$")) {
            throw new DomainExceptionInvariantViolation("SKU ID must be alphanumeric with hyphens allowed");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueSkuId)) return false;
        DomainValueSkuId that = (DomainValueSkuId) o;
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