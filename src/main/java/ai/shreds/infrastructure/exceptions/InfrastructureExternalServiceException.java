package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when external service calls fail in the infrastructure layer.
 */
public class InfrastructureExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;

    public InfrastructureExternalServiceException(String serviceName, String errorCode, Throwable cause) {
        super(String.format("External service call failed - Service: %s, Error Code: %s, Message: %s", 
                           serviceName, errorCode, cause.getMessage()), cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public InfrastructureExternalServiceException(String serviceName, String errorCode, String message) {
        super(String.format("External service call failed - Service: %s, Error Code: %s, Message: %s", 
                           serviceName, errorCode, message));
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public InfrastructureExternalServiceException(String serviceName, String errorCode, String message, Throwable cause) {
        super(String.format("External service call failed - Service: %s, Error Code: %s, Message: %s", 
                           serviceName, errorCode, message), cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("InfrastructureExternalServiceException{serviceName='%s', errorCode='%s', message='%s'}", 
                            serviceName, errorCode, getMessage());
    }
}