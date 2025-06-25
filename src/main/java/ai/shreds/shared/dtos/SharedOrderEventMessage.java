package ai.shreds.shared.dtos;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for order event messages.
 * Used for messaging between services when order status changes.
 */
public class SharedOrderEventMessage {
    
    private String eventId;
    private String orderId;
    private String eventType;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;
    
    // Default constructor
    public SharedOrderEventMessage() {}
    
    // All-args constructor
    public SharedOrderEventMessage(String eventId, String orderId, String eventType,
                                  String oldStatus, String newStatus, LocalDateTime timestamp,
                                  Map<String, Object> payload) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp;
        this.payload = payload;
    }
    
    // Getters and Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getOldStatus() {
        return oldStatus;
    }
    
    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
