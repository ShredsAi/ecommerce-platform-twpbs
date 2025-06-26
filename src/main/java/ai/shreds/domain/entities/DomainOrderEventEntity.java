package ai.shreds.domain.entities;

import ai.shreds.shared.dtos.SharedDomainEventDTO;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing an order event.
 * This is used for event sourcing and maintaining an audit trail of order-related activities.
 */
@Entity
@Table(name = "order_events")
public class DomainOrderEventEntity {
    
    @Id
    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId;
    
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_event_data", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> eventData;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "version")
    private Integer version;
    
    @Column(name = "source", length = 100)
    private String source;
    
    @Column(name = "correlation_id", length = 50)
    private String correlationId;
    
    // Event types
    public static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_TYPE_ORDER_UPDATED = "ORDER_UPDATED";
    public static final String EVENT_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String EVENT_TYPE_ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String EVENT_TYPE_ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String EVENT_TYPE_RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String EVENT_TYPE_RETURN_RECEIVED = "RETURN_RECEIVED";
    public static final String EVENT_TYPE_RETURN_COMPLETED = "RETURN_COMPLETED";
    public static final String EVENT_TYPE_PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    public static final String EVENT_TYPE_REFUND_PROCESSED = "REFUND_PROCESSED";
    
    // Default constructor for JPA
    protected DomainOrderEventEntity() {
        this.eventData = new HashMap<>();
    }
    
    // Constructor for creating new order events
    public DomainOrderEventEntity(String eventId, String orderId, String eventType, 
                                Map<String, Object> eventData, String source) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.eventData = convertObjectMapToStringMap(eventData);
        this.source = Objects.requireNonNull(source, "Source cannot be null");
        this.timestamp = LocalDateTime.now();
        this.version = 1;
    }
    
    // Constructor with correlation ID
    public DomainOrderEventEntity(String eventId, String orderId, String eventType, Map<String, Object> eventData, 
                                String source, String correlationId) {
        this(eventId, orderId, eventType, eventData, source);
        this.correlationId = correlationId;
    }
    
    /**
     * Business logic to add data to the event.
     */
    public void addEventData(String key, String value) {
        if (this.eventData == null) {
            this.eventData = new HashMap<>();
        }
        this.eventData.put(key, value);
    }
    
    /**
     * Business logic to get event data by key.
     */
    public String getEventDataValue(String key) {
        return eventData != null ? eventData.get(key) : null;
    }
    
    /**
     * Business logic to check if this event is of the specified type.
     */
    public boolean isEventType(String type) {
        return this.eventType != null && this.eventType.equals(type);
    }
    
    /**
     * Business logic to check if this event is from the specified source.
     */
    public boolean isFromSource(String source) {
        return this.source != null && this.source.equals(source);
    }
    
    /**
     * Business logic to check if this event is related to cancellations.
     */
    public boolean isCancellationEvent() {
        return this.eventType != null && (
            this.eventType.equals(EVENT_TYPE_ORDER_CANCELLED) ||
            this.eventType.contains("CANCELLATION")
        );
    }
    
    /**
     * Business logic to check if this event is related to returns.
     */
    public boolean isReturnEvent() {
        return this.eventType != null && (
            this.eventType.equals(EVENT_TYPE_RETURN_REQUESTED) ||
            this.eventType.equals(EVENT_TYPE_RETURN_RECEIVED) ||
            this.eventType.equals(EVENT_TYPE_RETURN_COMPLETED) ||
            this.eventType.contains("RETURN")
        );
    }
    
    /**
     * Convert object map to string map for JPA persistence.
     */
    private static Map<String, String> convertObjectMapToStringMap(Map<String, Object> objectMap) {
        Map<String, String> stringMap = new HashMap<>();
        if (objectMap != null) {
            objectMap.forEach((key, value) -> stringMap.put(key, value != null ? value.toString() : null));
        }
        return stringMap;
    }
    
    /**
     * Convert string map back to object map for DTO.
     */
    private Map<String, Object> convertStringMapToObjectMap() {
        Map<String, Object> objectMap = new HashMap<>();
        if (eventData != null) {
            eventData.forEach((key, value) -> objectMap.put(key, value));
        }
        return objectMap;
    }
    
    /**
     * Convert to DTO for external communication.
     */
    public SharedDomainEventDTO toDTO() {
        SharedDomainEventDTO dto = new SharedDomainEventDTO();
        dto.setEventId(this.eventId);
        dto.setAggregateId(this.orderId);
        dto.setEventType(this.eventType);
        dto.setEventData(convertStringMapToObjectMap());
        dto.setTimestamp(this.timestamp);
        dto.setVersion(this.version);
        dto.setSource(this.source);
        dto.setCorrelationId(this.correlationId);
        return dto;
    }
    
    /**
     * Create entity from DTO.
     */
    public static DomainOrderEventEntity fromDTO(SharedDomainEventDTO dto) {
        DomainOrderEventEntity entity = new DomainOrderEventEntity();
        entity.eventId = dto.getEventId();
        entity.orderId = dto.getAggregateId();
        entity.eventType = dto.getEventType();
        entity.eventData = convertObjectMapToStringMap(dto.getEventData());
        entity.timestamp = dto.getTimestamp();
        entity.version = dto.getVersion();
        entity.source = dto.getSource();
        entity.correlationId = dto.getCorrelationId();
        return entity;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
    public Map<String, Object> getEventData() { return convertStringMapToObjectMap(); }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Integer getVersion() { return version; }
    public String getSource() { return source; }
    public String getCorrelationId() { return correlationId; }
    
    // Setters for JPA
    protected void setEventId(String eventId) { this.eventId = eventId; }
    protected void setOrderId(String orderId) { this.orderId = orderId; }
    protected void setEventType(String eventType) { this.eventType = eventType; }
    protected void setEventData(Map<String, String> eventData) { this.eventData = eventData; }
    protected void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    protected void setVersion(Integer version) { this.version = version; }
    protected void setSource(String source) { this.source = source; }
    protected void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderEventEntity that = (DomainOrderEventEntity) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
    
    @Override
    public String toString() {
        return String.format("DomainOrderEventEntity{eventId='%s', orderId='%s', eventType='%s', timestamp=%s}", 
                           eventId, orderId, eventType, timestamp);
    }
}