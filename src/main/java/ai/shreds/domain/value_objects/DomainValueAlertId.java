package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class DomainValueAlertId {
    private final UUID value;

    public DomainValueAlertId(UUID value) {
        this.value = Objects.requireNonNull(value, "AlertId value cannot be null");
    }

    public static DomainValueAlertId create() {
        return new DomainValueAlertId(UUID.randomUUID());
    }

    public static DomainValueAlertId fromString(String id) {
        return new DomainValueAlertId(UUID.fromString(id));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueAlertId)) return false;
        DomainValueAlertId that = (DomainValueAlertId) o;
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