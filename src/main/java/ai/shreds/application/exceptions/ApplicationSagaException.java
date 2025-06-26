package ai.shreds.application.exceptions;

import java.util.UUID;

/**
 * Exception thrown during saga orchestration failures.
 */
public class ApplicationSagaException extends RuntimeException {
    private UUID sagaId;
    private String step;

    public ApplicationSagaException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationSagaException(UUID sagaId, String step, String message, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.step = step;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getStep() {
        return step;
    }
}
