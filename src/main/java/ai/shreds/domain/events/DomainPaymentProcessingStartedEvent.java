package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event representing the start of payment processing for a payment intent.
 */
public class DomainPaymentProcessingStartedEvent {
    private final DomainPaymentIntentIdValue intentId;
    private final LocalDateTime timestamp;

    public DomainPaymentProcessingStartedEvent(
            DomainPaymentIntentIdValue intentId,
            LocalDateTime timestamp) {
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentProcessingStartedEvent)) return false;
        DomainPaymentProcessingStartedEvent that = (DomainPaymentProcessingStartedEvent) o;
        return intentId.equals(that.intentId) && timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentId, timestamp);
    }

    @Override
    public String toString() {
        return "DomainPaymentProcessingStartedEvent{" +
                "intentId=" + intentId +
                ", timestamp=" + timestamp +
                '}';
    }
}