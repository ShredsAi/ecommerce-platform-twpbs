package ai.shreds.shared.dtos;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for domain event data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedDomainEventDTO {
    
    private String eventId;
    private String aggregateId;
    private String eventType;
    private Map<String, Object> eventData;
    private LocalDateTime timestamp;
    private Integer version;
    private String source;
    private String correlationId;
    
    // Default constructor
    public SharedDomainEventDTO() {}
    
    // All-args constructor
    public SharedDomainEventDTO(String eventId, String aggregateId, String eventType,
                               Map<String, Object> eventData, LocalDateTime timestamp,
                               Integer version, String source, String correlationId) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
        this.version = version;
        this.source = source;
        this.correlationId = correlationId;
    }
    
    // Getters and Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Map<String, Object> getEventData() {
        return eventData;
    }
    
    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Converts this DTO to domain object.
     * Implementation depends on the domain entity structure.
     */
    public Object toDomain() {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
    
    /**
     * Creates DTO from domain object.
     * Implementation depends on the domain entity structure.
     */
    public static SharedDomainEventDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
