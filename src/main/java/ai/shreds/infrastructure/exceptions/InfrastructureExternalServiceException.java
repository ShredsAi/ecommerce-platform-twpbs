package ai.shreds.infrastructure.exceptions;

public class InfrastructureExternalServiceException extends RuntimeException {
    private final String serviceName;
    private final String statusCode;
    private final Object requestData;

    public InfrastructureExternalServiceException(String message, Throwable cause, String serviceName, String statusCode, Object requestData) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.requestData = requestData;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public Object getRequestData() {
        return requestData;
    }
}