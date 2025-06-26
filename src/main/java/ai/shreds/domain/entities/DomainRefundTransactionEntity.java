package ai.shreds.domain.entities;

import java.time.Instant;

import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * Represents a refund transaction in the domain. 
 * This entity is used by the DomainOutputPortRefundTransactionRepository.
 */
public class DomainRefundTransactionEntity {

    private String refundId;
    private String orderId;
    private String cancellationId;
    private String returnId;
    private SharedMoneyValue amount;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public DomainRefundTransactionEntity() {
    }

    public DomainRefundTransactionEntity(String refundId, String orderId, String cancellationId, String returnId,
                                         SharedMoneyValue amount, String status, Instant createdAt, Instant updatedAt) {
        this.refundId = refundId;
        this.orderId = orderId;
        this.cancellationId = cancellationId;
        this.returnId = returnId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "DomainRefundTransactionEntity{" +
                "refundId='" + refundId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", cancellationId='" + cancellationId + '\'' +
                ", returnId='" + returnId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
