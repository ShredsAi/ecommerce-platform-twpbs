package ai.shreds.application.exceptions;

import java.util.List;

/**
 * Thrown when payment validation fails
 */
public class ApplicationPaymentValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ApplicationPaymentValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public ApplicationPaymentValidationException(String message) {
        super(message);
        this.validationErrors = List.of();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}