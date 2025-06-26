package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when a persistence operation fails.
 */
public class InfrastructurePersistenceException extends RuntimeException {

    private final String entityType;
    private final String operation;

    /**
     * Constructs the exception with entity type, operation, and cause.
     * @param entityType the type of the entity involved
     * @param operation the operation being performed (save, update, delete, etc.)
     * @param cause original exception cause
     */
    public InfrastructurePersistenceException(String entityType, String operation, Throwable cause) {
        super("Persistence error on entity " + entityType + " during " + operation, cause);
        this.entityType = entityType;
        this.operation = operation;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getOperation() {
        return operation;
    }
}
