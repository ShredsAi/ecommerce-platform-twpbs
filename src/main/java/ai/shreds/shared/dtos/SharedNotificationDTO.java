package ai.shreds.shared.dtos;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for notification data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedNotificationDTO {
    
    private String notificationId;
    private String recipientId;
    private String recipientEmail;
    private String type;
    private String subject;
    private String body;
    private String priority;
    private Map<String, Object> metadata;
    private LocalDateTime scheduledAt;
    
    // Default constructor
    public SharedNotificationDTO() {}
    
    // All-args constructor
    public SharedNotificationDTO(String notificationId, String recipientId, String recipientEmail,
                                String type, String subject, String body, String priority,
                                Map<String, Object> metadata, LocalDateTime scheduledAt) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.metadata = metadata;
        this.scheduledAt = scheduledAt;
    }
    
    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getRecipientId() {
        return recipientId;
    }
    
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
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
    public static SharedNotificationDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
