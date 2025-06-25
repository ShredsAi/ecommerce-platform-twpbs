package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for refund request data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedRefundRequestDTO {
    
    private String refundId;
    private String orderId;
    private String cancellationId;
    private String returnId;
    private SharedMoneyValue amount;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private Map<String, Object> metadata;
    
    // Default constructor
    public SharedRefundRequestDTO() {}
    
    // All-args constructor
    public SharedRefundRequestDTO(String refundId, String orderId, String cancellationId,
                                 String returnId, SharedMoneyValue amount, String reason,
                                 String status, LocalDateTime requestedAt,
                                 LocalDateTime processedAt, Map<String, Object> metadata) {
        this.refundId = refundId;
        this.orderId = orderId;
        this.cancellationId = cancellationId;
        this.returnId = returnId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public String getRefundId() {
        return refundId;
    }
    
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getCancellationId() {
        return cancellationId;
    }
    
    public void setCancellationId(String cancellationId) {
        this.cancellationId = cancellationId;
    }
    
    public String getReturnId() {
        return returnId;
    }
    
    public void setReturnId(String returnId) {
        this.returnId = returnId;
    }
    
    public SharedMoneyValue getAmount() {
        return amount;
    }
    
    public void setAmount(SharedMoneyValue amount) {
        this.amount = amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
    public static SharedRefundRequestDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
