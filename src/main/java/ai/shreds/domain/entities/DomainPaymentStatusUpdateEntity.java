package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentProcessorTypeEnum;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a payment status update for reconciliation purposes.
 * This entity tracks all status changes for payments to enable webhook reconciliation.
 */
public class DomainPaymentStatusUpdateEntity {
    private final Long id;
    private final DomainPaymentIdValue paymentId;
    private final DomainPaymentIntentIdValue intentId;
    private final DomainPaymentStatusEnum status;
    private final DomainPaymentProcessorTypeEnum processorType;
    private final LocalDateTime updatedAt;

    public DomainPaymentStatusUpdateEntity(
            Long id,
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainPaymentStatusEnum status,
            DomainPaymentProcessorTypeEnum processorType,
            LocalDateTime updatedAt) {
        this.id = id; // Can be null for new entities before persistence
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.processorType = Objects.requireNonNull(processorType, "processorType cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    /**
     * Static factory method to create a new PaymentStatusUpdate.
     */
    public static DomainPaymentStatusUpdateEntity create(
            DomainPaymentIdValue paymentId,
            DomainPaymentIntentIdValue intentId,
            DomainPaymentStatusEnum status,
            DomainPaymentProcessorTypeEnum processorType) {
        return new DomainPaymentStatusUpdateEntity(
                null, // ID will be assigned by persistence layer
                paymentId,
                intentId,
                status,
                processorType,
                LocalDateTime.now()
        );
    }

    /**
     * Creates a new status update from a payment intent.
     */
    public static DomainPaymentStatusUpdateEntity fromPaymentIntent(
            DomainPaymentIntentEntity paymentIntent,
            DomainPaymentIdValue paymentId) {
        return create(
                paymentId,
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getProcessorType()
        );
    }

    /**
     * Creates a new status update from a payment entity.
     */
    public static DomainPaymentStatusUpdateEntity fromPayment(
            DomainPaymentEntity payment) {
        return create(
                payment.getId(),
                payment.getPaymentIntentId(),
                payment.getStatus(),
                payment.getProcessorType()
        );
    }

    /**
     * Checks if this status update is for a terminal status.
     */
    public boolean isTerminalStatus() {
        return status == DomainPaymentStatusEnum.SUCCEEDED || status == DomainPaymentStatusEnum.FAILED;
    }

    /**
     * Checks if this status update indicates a successful payment.
     */
    public boolean isSuccessfulPayment() {
        return status == DomainPaymentStatusEnum.SUCCEEDED;
    }

    /**
     * Checks if this status update indicates a failed payment.
     */
    public boolean isFailedPayment() {
        return status == DomainPaymentStatusEnum.FAILED;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public DomainPaymentIdValue getPaymentId() {
        return paymentId;
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public DomainPaymentStatusEnum getStatus() {
        return status;
    }

    public DomainPaymentProcessorTypeEnum getProcessorType() {
        return processorType;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentStatusUpdateEntity)) return false;
        DomainPaymentStatusUpdateEntity that = (DomainPaymentStatusUpdateEntity) o;
        // If both have IDs, compare by ID, otherwise compare by business fields
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return paymentId.equals(that.paymentId) &&
                intentId.equals(that.intentId) &&
                status == that.status &&
                processorType == that.processorType &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(paymentId, intentId, status, processorType, updatedAt);
    }

    @Override
    public String toString() {
        return "DomainPaymentStatusUpdateEntity{" +
                "id=" + id +
                ", paymentId=" + paymentId +
                ", intentId=" + intentId +
                ", status=" + status +
                ", processorType=" + processorType +
                ", updatedAt=" + updatedAt +
                '}';
    }
}