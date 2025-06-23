package ai.shreds.domain.exceptions;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;

import java.time.LocalDateTime;

/**
 * Exception thrown when attempting to process an expired payment intent.
 */
public class DomainPaymentExpiredException extends RuntimeException {
    private final DomainPaymentIntentIdValue intentId;
    private final LocalDateTime expiredAt;

    public DomainPaymentExpiredException(
            DomainPaymentIntentIdValue intentId,
            LocalDateTime expiredAt) {
        super(String.format("Payment intent %s has expired at %s", intentId, expiredAt));
        this.intentId = intentId;
        this.expiredAt = expiredAt;
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
}