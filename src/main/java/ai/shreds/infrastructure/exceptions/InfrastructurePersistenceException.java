package ai.shreds.infrastructure.exceptions;

public class InfrastructurePersistenceException extends RuntimeException {
    private final String sqlError;
    private final String entity;

    public InfrastructurePersistenceException(String message, Throwable cause, String sqlError, String entity) {
        super(message, cause);
        this.sqlError = sqlError;
        this.entity = entity;
    }

    public String getSqlError() {
        return sqlError;
    }

    public String getEntity() {
        return entity;
    }
}