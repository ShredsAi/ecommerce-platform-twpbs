package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainPaymentMethodIdValue;
import ai.shreds.domain.value_objects.DomainPaymentProcessorTypeEnum;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a tokenized payment method.
 */
public class DomainPaymentTokenEntity {
    private final UUID id;
    private final DomainPaymentMethodIdValue paymentMethodId;
    private final String processorToken;
    private final DomainPaymentProcessorTypeEnum processorType;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DomainPaymentTokenEntity(
            UUID id,
            DomainPaymentMethodIdValue paymentMethodId,
            String processorToken,
            DomainPaymentProcessorTypeEnum processorType,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.paymentMethodId = Objects.requireNonNull(paymentMethodId, "paymentMethodId cannot be null");
        this.processorToken = Objects.requireNonNull(processorToken, "processorToken cannot be null");
        this.processorType = Objects.requireNonNull(processorType, "processorType cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        
        validateBusinessRules();
    }

    /**
     * Static factory method to create a new PaymentToken.
     */
    public static DomainPaymentTokenEntity create(
            UUID id,
            DomainPaymentMethodIdValue paymentMethodId,
            String processorToken,
            DomainPaymentProcessorTypeEnum processorType,
            LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new DomainPaymentTokenEntity(
                id,
                paymentMethodId,
                processorToken,
                processorType,
                expiresAt,
                now,
                now
        );
    }

    /**
     * Updates the token's updated timestamp.
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private void validateBusinessRules() {
        if (processorToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Processor token cannot be empty");
        }
        
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Token expiry cannot be before creation time");
        }
    }

    public UUID getId() {
        return id;
    }

    public DomainPaymentMethodIdValue getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getProcessorToken() {
        return processorToken;
    }

    public DomainPaymentProcessorTypeEnum getProcessorType() {
        return processorType;
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

    /**
     * Checks if the token has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the token is still valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentTokenEntity)) return false;
        DomainPaymentTokenEntity that = (DomainPaymentTokenEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainPaymentTokenEntity{" +
                "id=" + id +
                ", paymentMethodId=" + paymentMethodId +
                ", processorType=" + processorType +
                ", expiresAt=" + expiresAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}