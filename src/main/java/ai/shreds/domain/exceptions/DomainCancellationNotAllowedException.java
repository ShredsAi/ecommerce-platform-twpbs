package ai.shreds.domain.exceptions;

/**
 * Exception thrown when a cancellation request is not allowed by business rules.
 */
public class DomainCancellationNotAllowedException extends RuntimeException {
    private final String orderId;
    private final String reason;

    public DomainCancellationNotAllowedException(String message, String orderId, String reason) {
        super(message);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}