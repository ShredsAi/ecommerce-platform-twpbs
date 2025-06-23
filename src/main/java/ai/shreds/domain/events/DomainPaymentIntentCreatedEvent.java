package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;

import java.time.LocalDateTime;

/**
 * Domain event representing a newly created payment intent.
 */
public class DomainPaymentIntentCreatedEvent {
    private final DomainPaymentIntentIdValue intentId;
    private final DomainOrderIdValue orderId;
    private final DomainMoneyValue amount;
    private final LocalDateTime timestamp;

    public DomainPaymentIntentCreatedEvent(
            DomainPaymentIntentIdValue intentId,
            DomainOrderIdValue orderId,
            DomainMoneyValue amount,
            LocalDateTime timestamp) {
        this.intentId = intentId;
        this.orderId = orderId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public DomainMoneyValue getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}