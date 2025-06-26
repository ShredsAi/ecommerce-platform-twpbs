package ai.shreds.domain.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when domain entity validation fails.
 */
public class DomainValidationException extends RuntimeException {
    private final List<String> violations;

    public DomainValidationException(String message) {
        super(message);
        this.violations = new ArrayList<>();
        this.violations.add(message);
    }

    public DomainValidationException(List<String> violations) {
        super("Validation failed: " + String.join(", ", violations));
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }

    public DomainValidationException(String message, List<String> violations) {
        super(message);
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }

    public DomainValidationException(String message, String violation) {
        super(message);
        this.violations = new ArrayList<>();
        this.violations.add(violation);
    }

    /**
     * Gets the list of validation violations.
     * @return List of validation error messages
     */
    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    /**
     * Adds a violation to the list.
     * @param violation The violation message to add
     */
    public void addViolation(String violation) {
        if (violation != null && !violation.isBlank()) {
            this.violations.add(violation);
        }
    }

    @Override
    public String getMessage() {
        if (violations.isEmpty()) {
            return super.getMessage();
        }
        return super.getMessage() + ": " + String.join(", ", violations);
    }
}