package ai.shreds.application.exceptions;

import java.util.UUID;

/**
 * Exception thrown when processing of saga timeouts fails.
 */
public class ApplicationTimeoutException extends RuntimeException {
    private final UUID orderId;
    private final int retryCount;

    /**
     * Constructs an ApplicationTimeoutException without order context.
     * @param message the detail message
     */
    public ApplicationTimeoutException(String message) {
        super(message);
        this.orderId = null;
        this.retryCount = 0;
    }

    /**
     * Constructs an ApplicationTimeoutException with order context and retry count.
     * @param orderId the ID of the order that timed out
     * @param retryCount the number of retries attempted
     * @param message the detail message
     */
    public ApplicationTimeoutException(UUID orderId, int retryCount, String message) {
        super(message);
        this.orderId = orderId;
        this.retryCount = retryCount;
    }

    /**
     * Returns the order ID associated with the timeout error.
     * @return order ID or null if not set
     */
    public UUID getOrderId() {
        return orderId;
    }

    /**
     * Returns the retry count when the timeout occurred.
     * @return retry count
     */
    public int getRetryCount() {
        return retryCount;
    }
}