package ai.shreds.infrastructure.exceptions;

public class InfrastructureServiceException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;

    public InfrastructureServiceException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = extractServiceName(message);
        this.errorCode = extractErrorCode(cause);
    }

    public InfrastructureServiceException(String message, String serviceName, String errorCode) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public InfrastructureServiceException(String message, String serviceName, String errorCode, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    private String extractServiceName(String message) {
        if (message == null) {
            return "unknown";
        }
        if (message.toLowerCase().contains("payment")) return "PAYMENT_SERVICE";
        if (message.toLowerCase().contains("shipping")) return "SHIPPING_SERVICE";
        if (message.toLowerCase().contains("inventory")) return "INVENTORY_SERVICE";
        if (message.toLowerCase().contains("notification")) return "NOTIFICATION_SERVICE";
        if (message.toLowerCase().contains("kafka")) return "KAFKA_SERVICE";
        return "unknown";
    }

    private String extractErrorCode(Throwable cause) {
        if (cause == null) {
            return "UNKNOWN_ERROR";
        }
        String className = cause.getClass().getSimpleName();
        if (className.contains("Timeout")) return "SERVICE_TIMEOUT";
        if (className.contains("Connection")) return "CONNECTION_ERROR";
        if (className.contains("Http")) return "HTTP_ERROR";
        if (className.contains("Grpc")) return "GRPC_ERROR";
        return "SERVICE_ERROR";
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}