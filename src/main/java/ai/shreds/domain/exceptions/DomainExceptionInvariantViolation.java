package ai.shreds.domain.exceptions;

public class DomainExceptionInvariantViolation extends RuntimeException {
    
    public DomainExceptionInvariantViolation(String message) {
        super(message);
    }
    
    public DomainExceptionInvariantViolation(String message, Throwable cause) {
        super(message, cause);
    }
}