package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class DomainValueReconciliationId {
    private final UUID value;

    public DomainValueReconciliationId(UUID value) {
        this.value = Objects.requireNonNull(value, "ReconciliationId cannot be null");
    }

    public static DomainValueReconciliationId create() {
        return new DomainValueReconciliationId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueReconciliationId)) return false;
        DomainValueReconciliationId that = (DomainValueReconciliationId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}