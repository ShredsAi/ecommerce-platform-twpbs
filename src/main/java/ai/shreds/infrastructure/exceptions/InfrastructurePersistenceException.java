package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when persistence operations fail in the infrastructure layer.
 */
public class InfrastructurePersistenceException extends RuntimeException {

    private final String entityType;
    private final String operation;

    public InfrastructurePersistenceException(String entityType, String operation, Throwable cause) {
        super(String.format("Persistence operation failed - Entity: %s, Operation: %s, Message: %s", 
                           entityType, operation, cause.getMessage()), cause);
        this.entityType = entityType;
        this.operation = operation;
    }

    public InfrastructurePersistenceException(String entityType, String operation, String message) {
        super(String.format("Persistence operation failed - Entity: %s, Operation: %s, Message: %s", 
                           entityType, operation, message));
        this.entityType = entityType;
        this.operation = operation;
    }

    public InfrastructurePersistenceException(String entityType, String operation, String message, Throwable cause) {
        super(String.format("Persistence operation failed - Entity: %s, Operation: %s, Message: %s", 
                           entityType, operation, message), cause);
        this.entityType = entityType;
        this.operation = operation;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        return String.format("InfrastructurePersistenceException{entityType='%s', operation='%s', message='%s'}", 
                            entityType, operation, getMessage());
    }
}