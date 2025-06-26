package ai.shreds.shared.enums;

/**
 * Enumeration of possible order statuses shared across services.
 */
public enum SharedOrderStatusEnum {
    PENDING,
    CONFIRMED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}