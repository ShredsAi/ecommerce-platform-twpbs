package ai.shreds.application.exceptions;

public class ApplicationExceptionOptimisticLockException extends RuntimeException {

    public ApplicationExceptionOptimisticLockException(String message) {
        super(message);
    }

    public ApplicationExceptionOptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}