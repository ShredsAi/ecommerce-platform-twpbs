package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class DomainValueLedgerId {
    private final UUID value;

    public DomainValueLedgerId(UUID value) {
        this.value = Objects.requireNonNull(value, "LedgerId value cannot be null");
    }

    public static DomainValueLedgerId create() {
        return new DomainValueLedgerId(UUID.randomUUID());
    }

    public static DomainValueLedgerId fromString(String id) {
        return new DomainValueLedgerId(UUID.fromString(id));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueLedgerId)) return false;
        DomainValueLedgerId that = (DomainValueLedgerId) o;
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