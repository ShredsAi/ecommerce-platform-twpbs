package ai.shreds.infrastructure.exceptions;

import java.time.Instant;

public class InfrastructureExceptionExternalServiceError extends RuntimeException {

    private final String serviceName;
    private final String errorCode;
    private final Instant timestamp;
    private final String operation;

    public InfrastructureExceptionExternalServiceError(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = null;
        this.operation = null;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionExternalServiceError(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = null;
        this.operation = null;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionExternalServiceError(String serviceName, String message, String errorCode, String operation) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.operation = operation;
        this.timestamp = Instant.now();
    }

    public InfrastructureExceptionExternalServiceError(String serviceName, String message, Throwable cause, String errorCode, String operation) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.operation = operation;
        this.timestamp = Instant.now();
    }

    public String getServiceName() {
        return serviceName;
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
        sb.append(" [serviceName=").append(serviceName);
        if (errorCode != null) {
            sb.append(", errorCode=").append(errorCode);
        }
        if (operation != null) {
            sb.append(", operation=").append(operation);
        }
        sb.append(", timestamp=").append(timestamp).append("]");
        return sb.toString();
    }
}
