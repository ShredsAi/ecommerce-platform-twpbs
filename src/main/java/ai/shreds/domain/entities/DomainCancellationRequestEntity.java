package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.dtos.SharedCancellationRequestDTO;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a cancellation request aggregate.
 * Contains business logic for cancellation lifecycle management.
 */
@Entity
@Table(name = "cancellation_requests")
public class DomainCancellationRequestEntity {
    
    @Id
    @Column(name = "cancellation_id", nullable = false, length = 50)
    private String cancellationId;
    
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private SharedCancellationReasonEnum reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SharedCancellationStatusEnum status;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "refund_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "refund_currency"))
    })
    private SharedMoneyValue refundAmount;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Default constructor for JPA
    protected DomainCancellationRequestEntity() {}
    
    // Constructor for creating new cancellation requests
    public DomainCancellationRequestEntity(String cancellationId, String orderId, String customerId, 
                                         SharedCancellationReasonEnum reason, String notes) {
        this.cancellationId = Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.reason = Objects.requireNonNull(reason, "Reason cannot be null");
        this.notes = notes;
        this.status = SharedCancellationStatusEnum.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.version = 0L;
    }
    
    /**
     * Business logic to approve the cancellation request.
     * Validates current state and transitions to APPROVED.
     */
    public void approve() {
        if (this.status != SharedCancellationStatusEnum.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot approve cancellation in status %s. Expected PENDING.", this.status)
            );
        }
        this.status = SharedCancellationStatusEnum.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }
    
    /**
     * Business logic to reject the cancellation request.
     * @param rejectionReason The reason for rejection
     */
    public void reject(String rejectionReason) {
        if (this.status != SharedCancellationStatusEnum.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot reject cancellation in status %s. Expected PENDING.", this.status)
            );
        }
        this.status = SharedCancellationStatusEnum.REJECTED;
        this.notes = (this.notes != null ? this.notes + "; " : "") + "Rejected: " + rejectionReason;
    }
    
    /**
     * Business logic to complete the cancellation after all coordination tasks are done.
     */
    public void complete() {
        if (this.status != SharedCancellationStatusEnum.APPROVED && this.status != SharedCancellationStatusEnum.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot complete cancellation in status %s. Expected APPROVED or PROCESSING.", this.status)
            );
        }
        this.status = SharedCancellationStatusEnum.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark cancellation as processing during coordination phase.
     */
    public void markAsProcessing() {
        if (this.status != SharedCancellationStatusEnum.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot mark as processing cancellation in status %s. Expected APPROVED.", this.status)
            );
        }
        this.status = SharedCancellationStatusEnum.PROCESSING;
    }
    
    /**
     * Mark cancellation as failed during coordination phase.
     */
    public void markAsFailed() {
        if (this.status != SharedCancellationStatusEnum.PROCESSING && this.status != SharedCancellationStatusEnum.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot mark as failed cancellation in status %s. Expected APPROVED or PROCESSING.", this.status)
            );
        }
        this.status = SharedCancellationStatusEnum.FAILED;
    }
    
    /**
     * Set the refund amount for this cancellation.
     */
    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    /**
     * Check if the cancellation can be processed further.
     */
    public boolean canBeProcessed() {
        return this.status == SharedCancellationStatusEnum.PENDING || 
               this.status == SharedCancellationStatusEnum.APPROVED;
    }
    
    /**
     * Check if this cancellation is in a final state.
     */
    public boolean isFinalState() {
        return this.status == SharedCancellationStatusEnum.COMPLETED || 
               this.status == SharedCancellationStatusEnum.REJECTED || 
               this.status == SharedCancellationStatusEnum.FAILED;
    }
    
    /**
     * Convert to DTO for external communication.
     */
    public SharedCancellationRequestDTO toDTO() {
        SharedCancellationRequestDTO dto = new SharedCancellationRequestDTO();
        dto.setCancellationId(this.cancellationId);
        dto.setOrderId(this.orderId);
        dto.setCustomerId(this.customerId);
        dto.setReason(this.reason);
        dto.setStatus(this.status);
        dto.setRequestedAt(this.requestedAt);
        dto.setApprovedAt(this.approvedAt);
        dto.setCompletedAt(this.completedAt);
        dto.setRefundAmount(this.refundAmount);
        dto.setNotes(this.notes);
        return dto;
    }
    
    /**
     * Create entity from DTO.
     */
    public static DomainCancellationRequestEntity fromDTO(SharedCancellationRequestDTO dto) {
        DomainCancellationRequestEntity entity = new DomainCancellationRequestEntity();
        entity.cancellationId = dto.getCancellationId();
        entity.orderId = dto.getOrderId();
        entity.customerId = dto.getCustomerId();
        entity.reason = dto.getReason();
        entity.status = dto.getStatus();
        entity.requestedAt = dto.getRequestedAt();
        entity.approvedAt = dto.getApprovedAt();
        entity.completedAt = dto.getCompletedAt();
        entity.refundAmount = dto.getRefundAmount();
        entity.notes = dto.getNotes();
        return entity;
    }
    
    // Getters
    public String getCancellationId() { return cancellationId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public SharedCancellationReasonEnum getReason() { return reason; }
    public SharedCancellationStatusEnum getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public SharedMoneyValue getRefundAmount() { return refundAmount; }
    public String getNotes() { return notes; }
    public Long getVersion() { return version; }
    
    // Setters for JPA
    protected void setCancellationId(String cancellationId) { this.cancellationId = cancellationId; }
    protected void setOrderId(String orderId) { this.orderId = orderId; }
    protected void setCustomerId(String customerId) { this.customerId = customerId; }
    protected void setReason(SharedCancellationReasonEnum reason) { this.reason = reason; }
    protected void setStatus(SharedCancellationStatusEnum status) { this.status = status; }
    protected void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    protected void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    protected void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    protected void setNotes(String notes) { this.notes = notes; }
    protected void setVersion(Long version) { this.version = version; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCancellationRequestEntity that = (DomainCancellationRequestEntity) o;
        return Objects.equals(cancellationId, that.cancellationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cancellationId);
    }
    
    @Override
    public String toString() {
        return String.format("DomainCancellationRequestEntity{cancellationId='%s', orderId='%s', status=%s}", 
                           cancellationId, orderId, status);
    }
}