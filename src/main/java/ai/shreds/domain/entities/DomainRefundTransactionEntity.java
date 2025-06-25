package ai.shreds.domain.entities;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing a refund transaction.
 * Tracks the lifecycle of refund operations for cancellations and returns.
 */
@Entity
@Table(name = "refund_transactions")
public class DomainRefundTransactionEntity {
    
    @Id
    @Column(name = "refund_id", nullable = false, length = 50)
    private String refundId;
    
    @Column(name = "cancellation_id", length = 50)
    private String cancellationId;
    
    @Column(name = "return_id", length = 50)
    private String returnId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "refund_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "refund_currency", nullable = false))
    })
    private SharedMoneyValue amount;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "reason", length = 255)
    private String reason;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "refund_metadata", joinColumns = @JoinColumn(name = "refund_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    // Refund statuses
    public static final String STATUS_INITIATED = "INITIATED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // Default constructor for JPA
    protected DomainRefundTransactionEntity() {
        this.metadata = new HashMap<>();
    }
    
    // Constructor for cancellation refunds
    public DomainRefundTransactionEntity(String refundId, String cancellationId, 
                                       SharedMoneyValue amount, String reason) {
        this.refundId = Objects.requireNonNull(refundId, "Refund ID cannot be null");
        this.cancellationId = Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.reason = reason;
        this.status = STATUS_INITIATED;
        this.initiatedAt = LocalDateTime.now();
        this.metadata = new HashMap<>();
        
        validateAmount();
    }
    
    // Constructor for return refunds
    public DomainRefundTransactionEntity(String refundId, String returnId, 
                                       SharedMoneyValue amount, String reason, boolean isReturn) {
        this.refundId = Objects.requireNonNull(refundId, "Refund ID cannot be null");
        this.returnId = Objects.requireNonNull(returnId, "Return ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.reason = reason;
        this.status = STATUS_INITIATED;
        this.initiatedAt = LocalDateTime.now();
        this.metadata = new HashMap<>();
        
        validateAmount();
    }
    
    /**
     * Business logic to validate refund amount.
     */
    private void validateAmount() {
        if (amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
    }
    
    /**
     * Business logic to mark refund as processing.
     */
    public void markAsProcessing() {
        if (!STATUS_INITIATED.equals(this.status)) {
            throw new IllegalStateException(
                String.format("Cannot mark as processing refund in status %s. Expected INITIATED.", this.status)
            );
        }
        this.status = STATUS_PROCESSING;
        addMetadata("processing_started_at", LocalDateTime.now().toString());
    }
    
    /**
     * Business logic to mark refund as completed.
     */
    public void markAsCompleted() {
        if (!STATUS_PROCESSING.equals(this.status) && !STATUS_INITIATED.equals(this.status)) {
            throw new IllegalStateException(
                String.format("Cannot complete refund in status %s. Expected PROCESSING or INITIATED.", this.status)
            );
        }
        this.status = STATUS_COMPLETED;
        this.processedAt = LocalDateTime.now();
        addMetadata("completed_at", this.processedAt.toString());
    }
    
    /**
     * Business logic to mark refund as failed.
     */
    public void markAsFailed(String failureReason) {
        if (STATUS_COMPLETED.equals(this.status) || STATUS_CANCELLED.equals(this.status)) {
            throw new IllegalStateException(
                String.format("Cannot mark as failed refund in status %s.", this.status)
            );
        }
        this.status = STATUS_FAILED;
        this.processedAt = LocalDateTime.now();
        addMetadata("failure_reason", failureReason);
        addMetadata("failed_at", this.processedAt.toString());
    }
    
    /**
     * Business logic to cancel the refund.
     */
    public void cancel(String cancellationReason) {
        if (STATUS_COMPLETED.equals(this.status)) {
            throw new IllegalStateException("Cannot cancel a completed refund");
        }
        this.status = STATUS_CANCELLED;
        this.processedAt = LocalDateTime.now();
        addMetadata("cancellation_reason", cancellationReason);
        addMetadata("cancelled_at", this.processedAt.toString());
    }
    
    /**
     * Add metadata information.
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * Check if refund is in a terminal state.
     */
    public boolean isTerminalState() {
        return STATUS_COMPLETED.equals(this.status) || 
               STATUS_FAILED.equals(this.status) || 
               STATUS_CANCELLED.equals(this.status);
    }
    
    /**
     * Check if refund can be retried.
     */
    public boolean canBeRetried() {
        return STATUS_FAILED.equals(this.status) || STATUS_INITIATED.equals(this.status);
    }
    
    /**
     * Check if this is a cancellation refund.
     */
    public boolean isCancellationRefund() {
        return cancellationId != null;
    }
    
    /**
     * Check if this is a return refund.
     */
    public boolean isReturnRefund() {
        return returnId != null;
    }
    
    /**
     * Get the source ID (cancellation or return).
     */
    public String getSourceId() {
        return cancellationId != null ? cancellationId : returnId;
    }
    
    /**
     * Get the source type.
     */
    public String getSourceType() {
        return cancellationId != null ? "CANCELLATION" : "RETURN";
    }
    
    /**
     * Calculate processing duration if processed.
     */
    public long getProcessingDurationInSeconds() {
        if (processedAt != null && initiatedAt != null) {
            return java.time.Duration.between(initiatedAt, processedAt).getSeconds();
        }
        return 0L;
    }
    
    /**
     * Convert to DTO for external communication.
     */
    public SharedRefundRequestDTO toDTO() {
        SharedRefundRequestDTO dto = new SharedRefundRequestDTO();
        dto.setRefundId(this.refundId);
        dto.setOrderId(getOrderIdFromMetadata());
        dto.setCancellationId(this.cancellationId);
        dto.setReturnId(this.returnId);
        dto.setAmount(this.amount);
        dto.setReason(this.reason);
        dto.setStatus(this.status);
        dto.setRequestedAt(this.initiatedAt);
        dto.setProcessedAt(this.processedAt);
        dto.setMetadata(convertMetadataToObjectMap());
        return dto;
    }
    
    /**
     * Extract order ID from metadata if available.
     */
    private String getOrderIdFromMetadata() {
        if (metadata != null && metadata.containsKey("order_id")) {
            return metadata.get("order_id");
        }
        return null;
    }
    
    /**
     * Convert string metadata to object map for DTO.
     */
    private Map<String, Object> convertMetadataToObjectMap() {
        Map<String, Object> objectMap = new HashMap<>();
        if (metadata != null) {
            metadata.forEach((key, value) -> objectMap.put(key, value));
        }
        return objectMap;
    }
    
    /**
     * Create entity from DTO.
     */
    public static DomainRefundTransactionEntity fromDTO(SharedRefundRequestDTO dto) {
        DomainRefundTransactionEntity entity = new DomainRefundTransactionEntity();
        entity.refundId = dto.getRefundId();
        entity.cancellationId = dto.getCancellationId();
        entity.returnId = dto.getReturnId();
        entity.amount = dto.getAmount();
        entity.reason = dto.getReason();
        entity.status = dto.getStatus();
        entity.initiatedAt = dto.getRequestedAt();
        entity.processedAt = dto.getProcessedAt();
        entity.metadata = convertObjectMapToStringMap(dto.getMetadata());
        return entity;
    }
    
    /**
     * Convert object metadata to string map for JPA persistence.
     */
    private static Map<String, String> convertObjectMapToStringMap(Map<String, Object> objectMap) {
        Map<String, String> stringMap = new HashMap<>();
        if (objectMap != null) {
            objectMap.forEach((key, value) -> stringMap.put(key, value != null ? value.toString() : null));
        }
        return stringMap;
    }
    
    // Getters
    public String getRefundId() { return refundId; }
    public String getCancellationId() { return cancellationId; }
    public String getReturnId() { return returnId; }
    public SharedMoneyValue getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public String getReason() { return reason; }
    public Map<String, Object> getMetadata() {
        return convertMetadataToObjectMap();
    }
    
    // Setters for JPA
    protected void setRefundId(String refundId) { this.refundId = refundId; }
    protected void setCancellationId(String cancellationId) { this.cancellationId = cancellationId; }
    protected void setReturnId(String returnId) { this.returnId = returnId; }
    protected void setAmount(SharedMoneyValue amount) { this.amount = amount; }
    protected void setStatus(String status) { this.status = status; }
    protected void setInitiatedAt(LocalDateTime initiatedAt) { this.initiatedAt = initiatedAt; }
    protected void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    protected void setReason(String reason) { this.reason = reason; }
    protected void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainRefundTransactionEntity that = (DomainRefundTransactionEntity) o;
        return Objects.equals(refundId, that.refundId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(refundId);
    }
    
    @Override
    public String toString() {
        return String.format("DomainRefundTransactionEntity{refundId='%s', sourceType='%s', amount=%s, status='%s'}", 
                           refundId, getSourceType(), amount, status);
    }
}