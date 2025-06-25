package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when an external service call fails.
 */
public class InfrastructureExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;

    /**
     * Constructs the exception with service name, error code, and cause.
     * @param serviceName name of the external service
     * @param errorCode error code or message from the service
     * @param cause original exception cause
     */
    public InfrastructureExternalServiceException(String serviceName, String errorCode, Throwable cause) {
        super("External service error in " + serviceName + ": " + errorCode, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
