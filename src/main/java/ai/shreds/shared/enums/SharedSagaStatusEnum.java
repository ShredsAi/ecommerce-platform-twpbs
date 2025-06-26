package ai.shreds.shared.enums;

/**
 * Enumeration of saga statuses shared across services.
 */
public enum SharedSagaStatusEnum {
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    FAILED,
    TIMED_OUT
}