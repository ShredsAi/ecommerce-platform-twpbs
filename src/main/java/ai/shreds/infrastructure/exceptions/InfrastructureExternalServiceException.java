package ai.shreds.infrastructure.exceptions;

import lombok.Getter;

/**
 * Exception thrown when an external service call fails.
 * This includes payment processors (Stripe, PayPal, Square), 
 * token vault, 3D Secure services, etc.
 */
@Getter
public class InfrastructureExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;
    private final boolean isRetryable;
    private final Integer httpStatus;

    /**
     * Creates a new external service exception.
     *
     * @param serviceName The name of the external service that failed
     * @param errorCode The error code returned by the service
     * @param message Detailed message about what went wrong
     * @param isRetryable Whether this error is temporary and could be retried
     * @param httpStatus The HTTP status code, if applicable
     */
    public InfrastructureExternalServiceException(String serviceName, String errorCode, String message, boolean isRetryable, Integer httpStatus) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.isRetryable = isRetryable;
        this.httpStatus = httpStatus;
    }

    /**
     * Creates a new external service exception with cause.
     * 
     * @param serviceName The name of the external service that failed
     * @param errorCode The error code returned by the service
     * @param message Detailed message about what went wrong
     * @param cause The underlying exception that caused this failure
     * @param isRetryable Whether this error is temporary and could be retried
     * @param httpStatus The HTTP status code, if applicable
     */
    public InfrastructureExternalServiceException(String serviceName, String errorCode, String message, Throwable cause, boolean isRetryable, Integer httpStatus) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.isRetryable = isRetryable;
        this.httpStatus = httpStatus;
    }

    /**
     * Creates a network-related external service exception.
     * 
     * @param serviceName The name of the external service
     * @param cause The underlying network exception
     * @return A new exception instance configured for network issues
     */
    public static InfrastructureExternalServiceException networkError(String serviceName, Throwable cause) {
        return new InfrastructureExternalServiceException(
            serviceName, 
            "NETWORK_ERROR",
            "Network error while communicating with " + serviceName + ": " + cause.getMessage(),
            cause,
            true, // Network errors are typically retryable
            null
        );
    }
    
    /**
     * Creates an authentication error for external service access.
     * 
     * @param serviceName The name of the external service
     * @param errorCode The specific error code
     * @param message The error message
     * @return A new exception instance configured for authentication issues
     */
    public static InfrastructureExternalServiceException authenticationError(String serviceName, String errorCode, String message) {
        return new InfrastructureExternalServiceException(
            serviceName,
            errorCode,
            message,
            false, // Auth errors are not retryable
            401
        );
    }
    
    /**
     * Creates a rate limit error for external service access.
     * 
     * @param serviceName The name of the external service
     * @param message The error message
     * @return A new exception instance configured for rate limiting issues
     */
    public static InfrastructureExternalServiceException rateLimitError(String serviceName, String message) {
        return new InfrastructureExternalServiceException(
            serviceName,
            "RATE_LIMITED",
            message,
            true, // Rate limit errors are retryable after backoff
            429
        );
    }
    
    /**
     * Creates a service unavailable error.
     * 
     * @param serviceName The name of the external service
     * @param message The error message
     * @return A new exception instance configured for service unavailable issues
     */
    public static InfrastructureExternalServiceException serviceUnavailable(String serviceName, String message) {
        return new InfrastructureExternalServiceException(
            serviceName,
            "SERVICE_UNAVAILABLE",
            message,
            true, // Service unavailable errors are retryable
            503
        );
    }
}