package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class DomainValueReservationId {
    private final UUID value;

    public DomainValueReservationId(UUID value) {
        this.value = Objects.requireNonNull(value, "ReservationId value cannot be null");
    }

    public static DomainValueReservationId create() {
        return new DomainValueReservationId(UUID.randomUUID());
    }

    public static DomainValueReservationId fromString(String id) {
        return new DomainValueReservationId(UUID.fromString(id));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueReservationId)) return false;
        DomainValueReservationId that = (DomainValueReservationId) o;
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