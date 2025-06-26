package ai.shreds.application.exceptions;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when compensation actions fail during saga execution.
 */
public class ApplicationCompensationException extends RuntimeException {
    private final List<String> failedCompensations;

    /**
     * Constructs an ApplicationCompensationException with no details of failed steps.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ApplicationCompensationException(String message, Throwable cause) {
        super(message, cause);
        this.failedCompensations = Collections.emptyList();
    }

    /**
     * Constructs an ApplicationCompensationException with specific failed compensation identifiers.
     * @param message the detail message
     * @param cause the cause of the exception
     * @param failedCompensations list of compensation steps that failed
     */
    public ApplicationCompensationException(String message, Throwable cause, List<String> failedCompensations) {
        super(message, cause);
        this.failedCompensations = failedCompensations;
    }

    /**
     * Returns the list of compensation steps that failed.
     * @return list of failed compensation identifiers
     */
    public List<String> getFailedCompensations() {
        return failedCompensations;
    }
}