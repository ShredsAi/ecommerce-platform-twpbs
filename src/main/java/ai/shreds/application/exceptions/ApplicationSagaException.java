package ai.shreds.application.exceptions;

/**
 * Exception thrown when a saga operation fails within Application Services.
 */
public class ApplicationSagaException extends RuntimeException {

    private final String sagaId;
    private final String compensationStep;

    public ApplicationSagaException(String message, String sagaId, String compensationStep, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.compensationStep = compensationStep;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getCompensationStep() {
        return compensationStep;
    }
}
