package ai.shreds.application.exceptions;

/**
 * Thrown when an error occurs during payment processing
 */
public class ApplicationPaymentProcessingException extends RuntimeException {

    private final String processorError;
    private final boolean retryable;

    public ApplicationPaymentProcessingException(String message, String processorError, boolean retryable) {
        super(message);
        this.processorError = processorError;
        this.retryable = retryable;
    }

    public String getProcessorError() {
        return processorError;
    }

    public boolean isRetryable() {
        return retryable;
    }
}