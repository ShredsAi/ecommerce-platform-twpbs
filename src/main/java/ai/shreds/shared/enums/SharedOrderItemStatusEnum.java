package ai.shreds.shared.enums;

/**
 * Enumeration of order item statuses shared across services.
 */
public enum SharedOrderItemStatusEnum {
    PENDING,
    CONFIRMED,
    ALLOCATED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}