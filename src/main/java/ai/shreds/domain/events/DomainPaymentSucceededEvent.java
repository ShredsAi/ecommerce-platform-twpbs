package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event representing a successful payment.
 * This event is published when a payment has been successfully processed.
 */
public class DomainPaymentSucceededEvent {
    private final DomainPaymentIdValue paymentId;
    private final DomainPaymentIntentIdValue intentId;
    private final DomainOrderIdValue orderId;
    private final DomainMoneyValue amount;
    private final LocalDateTime timestamp;

    public DomainPaymentSucceededEvent(
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainOrderIdValue orderId,
            DomainMoneyValue amount,
            LocalDateTime timestamp) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    public DomainPaymentIdValue getPaymentId() {
        return paymentId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentSucceededEvent)) return false;
        DomainPaymentSucceededEvent that = (DomainPaymentSucceededEvent) o;
        return paymentId.equals(that.paymentId) && 
               intentId.equals(that.intentId) && 
               orderId.equals(that.orderId) && 
               amount.equals(that.amount) && 
               timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, intentId, orderId, amount, timestamp);
    }

    @Override
    public String toString() {
        return "DomainPaymentSucceededEvent{" +
                "paymentId=" + paymentId +
                ", intentId=" + intentId +
                ", orderId=" + orderId +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}