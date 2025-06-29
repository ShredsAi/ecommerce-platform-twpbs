package ai.shreds.domain.exceptions;

public class DomainExceptionEntityNotFound extends RuntimeException {
    
    private final String entityType;
    private final String id;
    
    public DomainExceptionEntityNotFound(String entityType, String id) {
        super(String.format("%s with ID %s not found", entityType, id));
        this.entityType = entityType;
        this.id = id;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getId() {
        return id;
    }
}