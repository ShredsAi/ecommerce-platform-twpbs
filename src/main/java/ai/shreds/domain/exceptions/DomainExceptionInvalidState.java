package ai.shreds.domain.exceptions;

public class DomainExceptionInvalidState extends RuntimeException {
    
    private final String currentState;
    private final String attemptedAction;
    
    public DomainExceptionInvalidState(String currentState, String attemptedAction) {
        super(String.format("Cannot perform %s when in state %s", attemptedAction, currentState));
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public String getAttemptedAction() {
        return attemptedAction;
    }
}