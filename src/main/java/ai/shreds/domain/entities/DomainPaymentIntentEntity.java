package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainInvalidStateException;
import ai.shreds.domain.exceptions.DomainPaymentExpiredException;
import ai.shreds.domain.value_objects.*;
import ai.shreds.application.dtos.ApplicationPaymentIntentDTO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a payment intent in the domain.
 * A payment intent represents the intention to collect a payment from a customer.
 */
public class DomainPaymentIntentEntity {
    private final DomainPaymentIntentIdValue id;
    private final DomainOrderIdValue orderId;
    private final DomainCustomerIdValue customerId;
    private final DomainMoneyValue amount;
    private DomainPaymentStatusEnum status;
    private final DomainPaymentMethodIdValue paymentMethodId; // FIXED: Can be null during initial creation
    private final DomainPaymentProcessorTypeEnum processorType;
    private final String clientSecret;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    // Changed visibility from private to public
    public DomainPaymentIntentEntity(
            DomainPaymentIntentIdValue id,
            DomainOrderIdValue orderId,
            DomainCustomerIdValue customerId,
            DomainMoneyValue amount,
            DomainPaymentStatusEnum status,
            DomainPaymentMethodIdValue paymentMethodId,
            DomainPaymentProcessorTypeEnum processorType,
            String clientSecret,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        // FIXED: Allow paymentMethodId to be null during initial creation from OrderPlaced events
        this.paymentMethodId = paymentMethodId; // Can be null initially
        this.processorType = Objects.requireNonNull(processorType, "processorType cannot be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.version = Objects.requireNonNull(version, "version cannot be null");

        validateBusinessRules();
    }

    /**
     * Static factory method to create a new PaymentIntent.
     */
    public static DomainPaymentIntentEntity create(
            DomainPaymentIntentIdValue id,
            DomainOrderIdValue orderId,
            DomainCustomerIdValue customerId,
            DomainMoneyValue amount,
            DomainPaymentMethodIdValue paymentMethodId,
            DomainPaymentProcessorTypeEnum processorType,
            String clientSecret,
            LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new DomainPaymentIntentEntity(
                id,
                orderId,
                customerId,
                amount,
                DomainPaymentStatusEnum.REQUIRES_PAYMENT_METHOD,
                paymentMethodId,
                processorType,
                clientSecret,
                expiresAt,
                now,
                now,
                0L
        );
    }

    /**
     * Static factory method for reconstruction from persistence.
     */
    public static DomainPaymentIntentEntity reconstruct(
            DomainPaymentIntentIdValue id,
            DomainOrderIdValue orderId,
            DomainCustomerIdValue customerId,
            DomainMoneyValue amount,
            DomainPaymentStatusEnum status,
            DomainPaymentMethodIdValue paymentMethodId,
            DomainPaymentProcessorTypeEnum processorType,
            String clientSecret,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        return new DomainPaymentIntentEntity(
                id,
                orderId,
                customerId,
                amount,
                status,
                paymentMethodId,
                processorType,
                clientSecret,
                expiresAt,
                createdAt,
                updatedAt,
                version
        );
    }

    // Getter methods
    public DomainPaymentIntentIdValue getId() {
        return id;
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public DomainCustomerIdValue getCustomerId() {
        return customerId;
    }

    public DomainMoneyValue getAmount() {
        return amount;
    }

    public DomainPaymentStatusEnum getStatus() {
        return status;
    }

    public DomainPaymentMethodIdValue getPaymentMethodId() {
        return paymentMethodId;
    }

    public DomainPaymentProcessorTypeEnum getProcessorType() {
        return processorType;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
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

    /**
     * Confirm the payment intent.
     */
    public void confirm() {
        if (status != DomainPaymentStatusEnum.REQUIRES_CONFIRMATION) {
            throw new DomainInvalidStateException(status, DomainPaymentStatusEnum.REQUIRES_CONFIRMATION);
        }
        // FIXED: Validate payment method is available before confirming
        if (paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method required before confirmation");
        }
        this.status = DomainPaymentStatusEnum.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancel the payment intent.
     */
    public void cancel() {
        if (status == DomainPaymentStatusEnum.SUCCEEDED || status == DomainPaymentStatusEnum.FAILED) {
            throw new DomainInvalidStateException(status, DomainPaymentStatusEnum.FAILED);
        }
        this.status = DomainPaymentStatusEnum.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the payment requires 3D Secure authentication.
     */
    public boolean requiresThreeDSecure() {
        // Business logic to determine if 3D Secure is required
        return amount.getAmount().compareTo(java.math.BigDecimal.valueOf(100)) > 0; // Example: amounts over 100 require 3DS
    }

    /**
     * Check if the payment intent is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Start processing of the payment intent.
     */
    public void startProcessing() {
        this.status = DomainPaymentStatusEnum.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark the payment intent as succeeded.
     */
    public void markSucceeded() {
        this.status = DomainPaymentStatusEnum.SUCCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark the payment intent as failed.
     */
    public void markFailed() {
        this.status = DomainPaymentStatusEnum.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the payment intent is in a terminal state (succeeded or failed).
     */
    public boolean isTerminal() {
        return status == DomainPaymentStatusEnum.SUCCEEDED || status == DomainPaymentStatusEnum.FAILED;
    }

    /**
     * Update the status of the payment intent.
     */
    public void updateStatus(DomainPaymentStatusEnum newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Convert to Application DTO.
     */
    public ApplicationPaymentIntentDTO toApplicationDTO() {
        return ApplicationPaymentIntentDTO.fromDomainEntity(this);
    }

    /**
     * Validates business rules for the payment intent.
     */
    private void validateBusinessRules() {
        // Validate amount is positive
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        // Validate expiration date is in the future
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration date must be after creation date");
        }

        // Validate client secret is not empty
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("Client secret cannot be null or empty");
        }
        
        // FIXED: Validate payment method consistency with status
        if (status == DomainPaymentStatusEnum.REQUIRES_CONFIRMATION && paymentMethodId == null) {
            throw new IllegalArgumentException("Payment method is required when status is REQUIRES_CONFIRMATION");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainPaymentIntentEntity that = (DomainPaymentIntentEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainPaymentIntentEntity{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", customerId=" + customerId +
                ", amount=" + amount +
                ", status=" + status +
                ", paymentMethodId=" + paymentMethodId +
                ", processorType=" + processorType +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}