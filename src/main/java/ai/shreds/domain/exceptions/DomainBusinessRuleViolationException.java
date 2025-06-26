package ai.shreds.domain.exceptions;

/**
 * Domain exception thrown when a business rule is violated.
 * This exception indicates that the requested operation violates established business policies.
 */
public class DomainBusinessRuleViolationException extends RuntimeException {
    
    private final String rule;
    private final String violationDetails;
    
    /**
     * Create a new business rule violation exception.
     * 
     * @param message the exception message
     * @param rule the name or identifier of the violated rule
     * @param violationDetails detailed information about the violation
     */
    public DomainBusinessRuleViolationException(String message, String rule, String violationDetails) {
        super(message);
        this.rule = rule;
        this.violationDetails = violationDetails;
    }
    
    /**
     * Create a new business rule violation exception with cause.
     * 
     * @param message the exception message
     * @param rule the name or identifier of the violated rule
     * @param violationDetails detailed information about the violation
     * @param cause the underlying cause of the exception
     */
    public DomainBusinessRuleViolationException(String message, String rule, String violationDetails, Throwable cause) {
        super(message, cause);
        this.rule = rule;
        this.violationDetails = violationDetails;
    }
    
    /**
     * Get the name or identifier of the violated rule.
     * 
     * @return the rule identifier
     */
    public String getRule() {
        return rule;
    }
    
    /**
     * Get detailed information about the rule violation.
     * 
     * @return the violation details
     */
    public String getViolationDetails() {
        return violationDetails;
    }
    
    @Override
    public String toString() {
        return String.format("%s{rule='%s', violationDetails='%s', message='%s'}", 
                           getClass().getSimpleName(), rule, violationDetails, getMessage());
    }
}