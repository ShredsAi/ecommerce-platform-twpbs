package ai.shreds.domain.exceptions;

import lombok.Getter;

@Getter
public class DomainInvariantViolationException extends RuntimeException {
    private final String invariant;
    private final Object actualValue;

    public DomainInvariantViolationException(String message, String invariant, Object actualValue) {
        super(message);
        this.invariant = invariant;
        this.actualValue = actualValue;
    }

    public DomainInvariantViolationException(String message, String invariant, Object actualValue, Throwable cause) {
        super(message, cause);
        this.invariant = invariant;
        this.actualValue = actualValue;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (invariant != null) {
            sb.append(" [Invariant: ").append(invariant).append("]");
        }
        if (actualValue != null) {
            sb.append(" [Actual Value: ").append(actualValue).append("]");
        }
        return sb.toString();
    }
}