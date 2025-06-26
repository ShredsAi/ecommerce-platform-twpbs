package ai.shreds.infrastructure.exceptions;

public class InfrastructureRepositoryException extends RuntimeException {

    private final String entityType;
    private final String operation;

    public InfrastructureRepositoryException(String message, Throwable cause) {
        super(message, cause);
        this.entityType = extractEntityType(message);
        this.operation = extractOperation(message);
    }

    public InfrastructureRepositoryException(String message, String entityType, String operation) {
        super(message);
        this.entityType = entityType;
        this.operation = operation;
    }

    public InfrastructureRepositoryException(String message, String entityType, String operation, Throwable cause) {
        super(message, cause);
        this.entityType = entityType;
        this.operation = operation;
    }

    private String extractEntityType(String message) {
        if (message == null) {
            return "unknown";
        }
        if (message.toLowerCase().contains("order")) return "ORDER";
        if (message.toLowerCase().contains("payment")) return "PAYMENT";
        if (message.toLowerCase().contains("shipping")) return "SHIPPING";
        if (message.toLowerCase().contains("saga")) return "SAGA_STATE";
        if (message.toLowerCase().contains("event")) return "ORDER_EVENT";
        return "unknown";
    }

    private String extractOperation(String message) {
        if (message == null) {
            return "unknown";
        }
        if (message.toLowerCase().contains("save") || message.toLowerCase().contains("insert")) return "SAVE";
        if (message.toLowerCase().contains("find") || message.toLowerCase().contains("select")) return "FIND";
        if (message.toLowerCase().contains("update")) return "UPDATE";
        if (message.toLowerCase().contains("delete")) return "DELETE";
        if (message.toLowerCase().contains("count")) return "COUNT";
        return "unknown";
    }

    public String getEntityType() {
        return entityType;
    }

    public String getOperation() {
        return operation;
    }
}