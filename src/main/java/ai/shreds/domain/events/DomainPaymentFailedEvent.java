package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.domain.value_objects.DomainOrderIdValue;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event representing a failed payment.
 * This event is published when a payment processing fails for any reason.
 */
public class DomainPaymentFailedEvent {
    private final DomainPaymentIdValue paymentId;
    private final DomainPaymentIntentIdValue intentId;
    private final DomainOrderIdValue orderId;
    private final String failureReason;
    private final LocalDateTime timestamp;

    public DomainPaymentFailedEvent(
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainOrderIdValue orderId,
            String failureReason,
            LocalDateTime timestamp) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.failureReason = failureReason != null ? failureReason : "Unknown failure";
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        
        validateEvent();
    }

    /**
     * Creates a payment failed event with a specific failure reason.
     */
    public static DomainPaymentFailedEvent withReason(
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainOrderIdValue orderId,
            String failureReason) {
        return new DomainPaymentFailedEvent(
            paymentId,
            intentId,
            orderId,
            failureReason,
            LocalDateTime.now()
        );
    }

    /**
     * Creates a payment failed event with unknown reason.
     */
    public static DomainPaymentFailedEvent withUnknownReason(
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainOrderIdValue orderId) {
        return new DomainPaymentFailedEvent(
            paymentId,
            intentId,
            orderId,
            "Unknown failure",
            LocalDateTime.now()
        );
    }

    private void validateEvent() {
        if (failureReason != null && failureReason.trim().isEmpty()) {
            throw new IllegalArgumentException("failureReason cannot be empty string");
        }
        
        if (timestamp.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new IllegalArgumentException("timestamp cannot be in the future");
        }
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

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this failure is due to insufficient funds.
     */
    public boolean isInsufficientFunds() {
        return failureReason != null && 
               failureReason.toLowerCase().contains("insufficient");
    }

    /**
     * Checks if this failure is due to card decline.
     */
    public boolean isCardDeclined() {
        return failureReason != null && 
               (failureReason.toLowerCase().contains("declined") ||
                failureReason.toLowerCase().contains("reject"));
    }

    /**
     * Checks if this failure is due to processor error.
     */
    public boolean isProcessorError() {
        return failureReason != null && 
               (failureReason.toLowerCase().contains("processor") ||
                failureReason.toLowerCase().contains("gateway"));
    }

    /**
     * Checks if this failure reason indicates a retryable error.
     */
    public boolean isRetryable() {
        if (failureReason == null) {
            return false;
        }
        
        String reason = failureReason.toLowerCase();
        return reason.contains("timeout") ||
               reason.contains("network") ||
               reason.contains("temporary") ||
               reason.contains("rate limit");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentFailedEvent)) return false;
        DomainPaymentFailedEvent that = (DomainPaymentFailedEvent) o;
        return paymentId.equals(that.paymentId) && 
               intentId.equals(that.intentId) && 
               orderId.equals(that.orderId) && 
               Objects.equals(failureReason, that.failureReason) && 
               timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, intentId, orderId, failureReason, timestamp);
    }

    @Override
    public String toString() {
        return "DomainPaymentFailedEvent{" +
                "paymentId=" + paymentId +
                ", intentId=" + intentId +
                ", orderId=" + orderId +
                ", failureReason='" + failureReason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}