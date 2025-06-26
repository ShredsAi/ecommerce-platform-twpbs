package ai.shreds.shared.enums;

/**
 * Enumeration of event types shared across services.
 */
public enum SharedEventTypeEnum {
    ORDER_CREATED,
    PAYMENT_INITIATED,
    PAYMENT_AUTHORIZED,
    PAYMENT_CAPTURED,
    PAYMENT_FAILED,
    SHIPPING_ARRANGED,
    SHIPPING_FAILED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    TIMEOUT_HANDLED,
    SAGA_TIMEOUT_EXHAUSTED
}