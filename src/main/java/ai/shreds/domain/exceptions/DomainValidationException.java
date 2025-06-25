package ai.shreds.domain.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class DomainValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public DomainValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public DomainValidationException(String message, List<String> validationErrors, Throwable cause) {
        super(message, cause);
        this.validationErrors = validationErrors;
    }

    @Override
    public String getMessage() {
        if (validationErrors != null && !validationErrors.isEmpty()) {
            return super.getMessage() + ": " + String.join(", ", validationErrors);
        }
        return super.getMessage();
    }
}