package ai.shreds.infrastructure.exceptions;

public class InfrastructureExceptionRepositoryException extends RuntimeException {

    private final String operation;
    private final String entityType;

    public InfrastructureExceptionRepositoryException(String operation, String entityType, Throwable cause) {
        super(String.format("Error during '%s' on entity '%s'", operation, entityType), cause);
        this.operation = operation;
        this.entityType = entityType;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntityType() {
        return entityType;
    }
}
