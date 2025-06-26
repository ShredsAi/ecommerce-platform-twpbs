package ai.shreds.domain.exceptions;

/**
 * Domain exception thrown when an invalid state transition is attempted.
 * This exception indicates that the requested state change is not allowed.
 */
public class DomainInvalidStateTransitionException extends RuntimeException {
    
    private final String entityId;
    private final String currentState;
    private final String targetState;
    
    /**
     * Create a new invalid state transition exception.
     * 
     * @param message the exception message
     * @param entityId the ID of the entity with invalid state transition
     * @param currentState the current state of the entity
     * @param targetState the attempted target state
     */
    public DomainInvalidStateTransitionException(String message, String entityId, String currentState, String targetState) {
        super(message);
        this.entityId = entityId;
        this.currentState = currentState;
        this.targetState = targetState;
    }
    
    /**
     * Create a new invalid state transition exception with cause.
     * 
     * @param message the exception message
     * @param entityId the ID of the entity with invalid state transition
     * @param currentState the current state of the entity
     * @param targetState the attempted target state
     * @param cause the underlying cause of the exception
     */
    public DomainInvalidStateTransitionException(String message, String entityId, String currentState, String targetState, Throwable cause) {
        super(message, cause);
        this.entityId = entityId;
        this.currentState = currentState;
        this.targetState = targetState;
    }
    
    /**
     * Get the entity ID that had the invalid state transition.
     * 
     * @return the entity ID
     */
    public String getEntityId() {
        return entityId;
    }
    
    /**
     * Get the current state of the entity.
     * 
     * @return the current state
     */
    public String getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the attempted target state.
     * 
     * @return the target state
     */
    public String getTargetState() {
        return targetState;
    }
    
    @Override
    public String toString() {
        return String.format("%s{entityId='%s', currentState='%s', targetState='%s', message='%s'}", 
                           getClass().getSimpleName(), entityId, currentState, targetState, getMessage());
    }
}