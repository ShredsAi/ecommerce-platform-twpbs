package ai.shreds.shared.dtos;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for system-triggered cancellation messages.
 * Used for JMS/Kafka messaging between services.
 */
public class SharedSystemCancellationMessage {
    
    private String messageId;
    private String orderId;
    private String reason;
    private String source;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    // Default constructor
    public SharedSystemCancellationMessage() {}
    
    // All-args constructor
    public SharedSystemCancellationMessage(String messageId, String orderId, String reason,
                                          String source, LocalDateTime timestamp,
                                          Map<String, Object> metadata) {
        this.messageId = messageId;
        this.orderId = orderId;
        this.reason = reason;
        this.source = source;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
