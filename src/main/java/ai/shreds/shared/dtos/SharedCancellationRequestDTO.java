package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.time.LocalDateTime;

/**
 * DTO for cancellation request data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedCancellationRequestDTO {
    
    private String cancellationId;
    private String orderId;
    private String customerId;
    private SharedCancellationReasonEnum reason;
    private SharedCancellationStatusEnum status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private SharedMoneyValue refundAmount;
    private String notes;
    
    // Default constructor
    public SharedCancellationRequestDTO() {}
    
    // All-args constructor
    public SharedCancellationRequestDTO(String cancellationId, String orderId, String customerId,
                                       SharedCancellationReasonEnum reason, SharedCancellationStatusEnum status,
                                       LocalDateTime requestedAt, LocalDateTime approvedAt, LocalDateTime completedAt,
                                       SharedMoneyValue refundAmount, String notes) {
        this.cancellationId = cancellationId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
        this.status = status;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.completedAt = completedAt;
        this.refundAmount = refundAmount;
        this.notes = notes;
    }
    
    // Getters and Setters
    public String getCancellationId() {
        return cancellationId;
    }
    
    public void setCancellationId(String cancellationId) {
        this.cancellationId = cancellationId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public SharedCancellationReasonEnum getReason() {
        return reason;
    }
    
    public void setReason(SharedCancellationReasonEnum reason) {
        this.reason = reason;
    }
    
    public SharedCancellationStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(SharedCancellationStatusEnum status) {
        this.status = status;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public SharedMoneyValue getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
    public static SharedCancellationRequestDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
