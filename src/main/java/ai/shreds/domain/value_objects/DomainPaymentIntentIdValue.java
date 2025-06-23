package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a PaymentIntent identifier.
 */
public final class DomainPaymentIntentIdValue {

    private final UUID value;

    public DomainPaymentIntentIdValue(UUID value) {
        this.value = Objects.requireNonNull(value, "PaymentIntentIdValue cannot be null");
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainPaymentIntentIdValue that = (DomainPaymentIntentIdValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}