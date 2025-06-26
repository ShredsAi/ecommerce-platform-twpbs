package ai.shreds.shared.enums;

/**
 * Enumeration of steps in the orchestration saga shared across services.
 */
public enum SharedSagaStepEnum {
    PAYMENT_AUTHORIZATION,
    PAYMENT_CAPTURE,
    SHIPPING_ARRANGEMENT,
    INVENTORY_ALLOCATION,
    NOTIFICATION_SENDING,
    COMPENSATION_PAYMENT,
    COMPENSATION_SHIPPING,
    COMPENSATION_INVENTORY
}