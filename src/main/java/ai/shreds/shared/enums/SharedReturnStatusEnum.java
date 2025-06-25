package ai.shreds.shared.enums;

/**
 * Enumeration of possible return request statuses.
 * Fixed typo: IN_TRANIST -> IN_TRANSIT
 */
public enum SharedReturnStatusEnum {
    REQUESTED,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    RECEIVED,
    PROCESSING,
    REFUNDED,
    CLOSED
}
