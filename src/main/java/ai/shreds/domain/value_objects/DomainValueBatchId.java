package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueBatchId {
    private final String value;

    public DomainValueBatchId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Batch ID cannot be null or empty");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueBatchId)) return false;
        DomainValueBatchId that = (DomainValueBatchId) o;
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