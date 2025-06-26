package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.time.LocalDateTime;

/**
 * DTO for cancellation response data transfer.
 */
public class SharedCancellationResponseDTO {
    
    private String cancellationId;
    private String orderId;
    private String status;
    private String reason;
    private LocalDateTime requestedAt;
    private SharedMoneyValue refundAmount;
    private String message;
    private Boolean success;
    
    // Default constructor
    public SharedCancellationResponseDTO() {}
    
    // All-args constructor
    public SharedCancellationResponseDTO(String cancellationId, String orderId, String status,
                                       String reason, LocalDateTime requestedAt, SharedMoneyValue refundAmount,
                                       String message, Boolean success) {
        this.cancellationId = cancellationId;
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.refundAmount = refundAmount;
        this.message = message;
        this.success = success;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public SharedMoneyValue getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
}