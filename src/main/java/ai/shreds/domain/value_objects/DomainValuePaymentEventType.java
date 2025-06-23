package ai.shreds.domain.value_objects;

/**
 * Enumeration of payment event types used in domain events.
 */
public enum DomainValuePaymentEventType {
    PAYMENT_INTENT_CREATED,
    PAYMENT_PROCESSING_STARTED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    THREE_D_SECURE_REQUIRED
}