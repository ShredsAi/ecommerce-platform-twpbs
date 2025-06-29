package ai.shreds.infrastructure.exceptions;

import java.time.Instant;

public class InfrastructureExceptionDatabaseError extends RuntimeException {

    private final String errorCode;
    private final Instant timestamp;
    private final String operation;

    public InfrastructureExceptionDatabaseError(String message) {
        super(message);
        this.errorCode = null;
        this.operation = null;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionDatabaseError(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.operation = null;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionDatabaseError(String message, String errorCode, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionDatabaseError(String message, Throwable cause, String errorCode, String operation) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
        this.timestamp = Instant.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOperation() {
        return operation;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(": ").append(getMessage());
        if (errorCode != null) {
            sb.append(" [errorCode=").append(errorCode);
        }
        if (operation != null) {
            sb.append(", operation=").append(operation);
        }
        sb.append(", timestamp=").append(timestamp).append("]");
        return sb.toString();
    }
}
