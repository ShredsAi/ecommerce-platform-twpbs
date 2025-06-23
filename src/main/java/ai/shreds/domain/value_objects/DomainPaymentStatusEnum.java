package ai.shreds.domain.value_objects;

/**
 * Enumeration of valid payment intent and payment statuses in the domain.
 */
public enum DomainPaymentStatusEnum {
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CONFIRMATION,
    PROCESSING,
    SUCCEEDED,
    FAILED
}