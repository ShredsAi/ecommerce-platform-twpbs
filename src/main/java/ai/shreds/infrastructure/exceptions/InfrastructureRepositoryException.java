package ai.shreds.infrastructure.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a repository operation fails.
 * This might be due to database connectivity issues,
 * constraint violations, or other persistence-related problems.
 */
@Getter
public class InfrastructureRepositoryException extends RuntimeException {

    private final String entityType;
    private final String operation;
    private final Throwable cause;

    /**
     * Creates a new repository exception.
     *
     * @param entityType The type of entity being operated on (e.g., "PaymentIntent")
     * @param operation The repository operation that failed (e.g., "save", "find")
     * @param message Detailed message about what went wrong
     * @param cause The underlying exception that caused this failure
     */
    public InfrastructureRepositoryException(String entityType, String operation, String message, Throwable cause) {
        super(message, cause);
        this.entityType = entityType;
        this.operation = operation;
        this.cause = cause;
    }

    /**
     * Creates a not found exception.
     *
     * @param entityType The type of entity
     * @param id The ID that was not found
     * @return A new exception instance for entity not found
     */
    public static InfrastructureRepositoryException notFound(String entityType, String id) {
        return new InfrastructureRepositoryException(
            entityType,
            "find",
            entityType + " with ID " + id + " not found",
            null
        );
    }

    /**
     * Creates a duplicate entity exception.
     *
     * @param entityType The type of entity
     * @param identifier The unique identifier that was duplicated
     * @return A new exception instance for duplicate entity
     */
    public static InfrastructureRepositoryException duplicateEntity(String entityType, String identifier) {
        return new InfrastructureRepositoryException(
            entityType,
            "save",
            "Duplicate " + entityType + " with identifier " + identifier,
            null
        );
    }

    /**
     * Creates a database connectivity exception.
     *
     * @param entityType The type of entity
     * @param operation The operation that failed
     * @param cause The underlying database exception
     * @return A new exception instance for database connection issues
     */
    public static InfrastructureRepositoryException databaseConnectivity(String entityType, String operation, Throwable cause) {
        return new InfrastructureRepositoryException(
            entityType,
            operation,
            "Database connectivity error during " + operation + " operation on " + entityType + ": " + cause.getMessage(),
            cause
        );
    }

    /**
     * Creates an optimistic locking exception.
     *
     * @param entityType The type of entity
     * @param id The entity ID
     * @param cause The underlying exception
     * @return A new exception instance for optimistic locking failures
     */
    public static InfrastructureRepositoryException optimisticLockingFailure(String entityType, String id, Throwable cause) {
        return new InfrastructureRepositoryException(
            entityType,
            "update",
            "Optimistic locking failure on " + entityType + " with ID " + id + ": the entity was modified by another transaction",
            cause
        );
    }
}