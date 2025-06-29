package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueVendorSku {
    private final String value;

    public DomainValueVendorSku(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Vendor SKU cannot be null or empty");
        }
        if (value.length() > 100) {
            throw new DomainExceptionInvariantViolation("Vendor SKU cannot exceed 100 characters");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueVendorSku)) return false;
        DomainValueVendorSku that = (DomainValueVendorSku) o;
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