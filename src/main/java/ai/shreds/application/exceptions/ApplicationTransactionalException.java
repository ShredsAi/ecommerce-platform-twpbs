package ai.shreds.application.exceptions;

/**
 * Exception thrown when a transactional operation fails within Application Services.
 */
public class ApplicationTransactionalException extends RuntimeException {

    private final String transactionId;
    private final String failedOperation;

    public ApplicationTransactionalException(String message, String transactionId, String failedOperation) {
        super(message);
        this.transactionId = transactionId;
        this.failedOperation = failedOperation;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailedOperation() {
        return failedOperation;
    }
}
