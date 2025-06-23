package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.*;
import ai.shreds.application.dtos.ApplicationPaymentDetailsDTO;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a processed payment.
 */
public class DomainPaymentEntity {
    private final DomainPaymentIdValue id;
    private final DomainPaymentIntentIdValue paymentIntentId;
    private final DomainMoneyValue amount;
    private DomainPaymentStatusEnum status;
    private final DomainPaymentProcessorTypeEnum processorType;
    private final DomainProcessorResponseValue processorResponse;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    private DomainPaymentEntity(
            DomainPaymentIdValue id,
            DomainPaymentIntentIdValue paymentIntentId,
            DomainMoneyValue amount,
            DomainPaymentStatusEnum status,
            DomainPaymentProcessorTypeEnum processorType,
            DomainProcessorResponseValue processorResponse,
            LocalDateTime processedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.paymentIntentId = Objects.requireNonNull(paymentIntentId, "paymentIntentId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.processorType = Objects.requireNonNull(processorType, "processorType cannot be null");
        this.processorResponse = Objects.requireNonNull(processorResponse, "processorResponse cannot be null");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.version = Objects.requireNonNull(version, "version cannot be null");
        
        validateBusinessRules();
    }

    /**
     * Static factory method to create a new Payment.
     */
    public static DomainPaymentEntity create(
            DomainPaymentIdValue id,
            DomainPaymentIntentIdValue paymentIntentId,
            DomainMoneyValue amount,
            DomainPaymentStatusEnum status,
            DomainPaymentProcessorTypeEnum processorType,
            DomainProcessorResponseValue processorResponse,
            LocalDateTime processedAt) {
        LocalDateTime now = LocalDateTime.now();
        return new DomainPaymentEntity(
                id,
                paymentIntentId,
                amount,
                status,
                processorType,
                processorResponse,
                processedAt,
                now,
                now,
                0L
        );
    }

    /**
     * Static factory method for reconstruction from persistence.
     */
    public static DomainPaymentEntity reconstruct(
            DomainPaymentIdValue id,
            DomainPaymentIntentIdValue paymentIntentId,
            DomainMoneyValue amount,
            DomainPaymentStatusEnum status,
            DomainPaymentProcessorTypeEnum processorType,
            DomainProcessorResponseValue processorResponse,
            LocalDateTime processedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        return new DomainPaymentEntity(
                id,
                paymentIntentId,
                amount,
                status,
                processorType,
                processorResponse,
                processedAt,
                createdAt,
                updatedAt,
                version
        );
    }

    /**
     * Updates the payment status.
     */
    public void updateStatus(DomainPaymentStatusEnum newStatus) {
        if (isTerminalStatus()) {
            throw new IllegalStateException("Cannot update status of payment in terminal state: " + status);
        }
        
        validateStatusTransition(newStatus);
        
        this.status = Objects.requireNonNull(newStatus, "status cannot be null");
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    /**
     * Marks the payment as successful.
     */
    public void markSuccessful() {
        updateStatus(DomainPaymentStatusEnum.SUCCEEDED);
    }

    /**
     * Marks the payment as failed.
     */
    public void markFailed() {
        updateStatus(DomainPaymentStatusEnum.FAILED);
    }

    /**
     * Checks if the payment can be updated.
     */
    public boolean canBeUpdated() {
        return !isTerminalStatus();
    }

    /**
     * Gets the processor transaction ID if available.
     */
    public String getProcessorTransactionId() {
        return processorResponse.getProcessorId();
    }

    /**
     * Gets the decline code if payment failed.
     */
    public String getDeclineCode() {
        if (!isFailed()) {
            return null;
        }
        return processorResponse.getResponseCode();
    }

    /**
     * Converts this domain entity to an ApplicationPaymentDetailsDTO.
     */
    public ApplicationPaymentDetailsDTO toApplicationDTO() {
        return ApplicationPaymentDetailsDTO.fromDomainEntity(this);
    }

    private boolean isTerminalStatus() {
        return status == DomainPaymentStatusEnum.SUCCEEDED || status == DomainPaymentStatusEnum.FAILED;
    }

    private void validateStatusTransition(DomainPaymentStatusEnum newStatus) {
        // Payments can only transition to terminal states
        if (newStatus != DomainPaymentStatusEnum.SUCCEEDED && newStatus != DomainPaymentStatusEnum.FAILED) {
            throw new IllegalArgumentException("Payment can only transition to SUCCEEDED or FAILED status");
        }
        
        // Cannot change from one terminal state to another
        if (isTerminalStatus()) {
            throw new IllegalArgumentException("Cannot change status from terminal state " + status + " to " + newStatus);
        }
    }

    private void validateBusinessRules() {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        // Only terminal statuses are allowed for payments
        if (status != DomainPaymentStatusEnum.SUCCEEDED && status != DomainPaymentStatusEnum.FAILED) {
            throw new IllegalArgumentException("Payment status must be SUCCEEDED or FAILED");
        }
        
        if (processedAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Processed date cannot be in the future");
        }
        
        if (processedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Processed date cannot be before creation date");
        }
    }

    // Getters
    public DomainPaymentIdValue getId() {
        return id;
    }

    public DomainPaymentIntentIdValue getPaymentIntentId() {
        return paymentIntentId;
    }

    public DomainMoneyValue getAmount() {
        return amount;
    }

    public DomainPaymentStatusEnum getStatus() {
        return status;
    }

    public DomainPaymentProcessorTypeEnum getProcessorType() {
        return processorType;
    }

    public DomainProcessorResponseValue getProcessorResponse() {
        return processorResponse;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isSuccessful() {
        return status == DomainPaymentStatusEnum.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == DomainPaymentStatusEnum.FAILED;
    }

    public String getFailureReason() {
        if (!isFailed()) {
            return null;
        }
        return processorResponse.getResponseMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentEntity)) return false;
        DomainPaymentEntity that = (DomainPaymentEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainPaymentEntity{" +
                "id=" + id +
                ", paymentIntentId=" + paymentIntentId +
                ", amount=" + amount +
                ", status=" + status +
                ", processorType=" + processorType +
                ", processedAt=" + processedAt +
                '}';
    }
}