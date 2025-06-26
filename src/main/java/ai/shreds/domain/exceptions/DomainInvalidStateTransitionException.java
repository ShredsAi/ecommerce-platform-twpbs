package ai.shreds.domain.exceptions;

/**
 * Exception thrown when an invalid state transition is attempted on a domain entity.
 */
public class DomainInvalidStateTransitionException extends RuntimeException {
    private final String currentStatus;
    private final String targetStatus;

    public DomainInvalidStateTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Invalid state transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public DomainInvalidStateTransitionException(String currentStatus, String targetStatus, String reason) {
        super(String.format("Invalid state transition from %s to %s: %s", currentStatus, targetStatus, reason));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetStatus() {
        return targetStatus;
    }
}