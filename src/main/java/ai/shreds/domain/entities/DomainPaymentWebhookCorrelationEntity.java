package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainPaymentIdValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing a webhook correlation for payment reconciliation.
 * This entity tracks webhook deliveries and correlates them with payments.
 */
public class DomainPaymentWebhookCorrelationEntity {
    private final Long id;
    private final String webhookId;
    private DomainPaymentIdValue paymentId;
    private final LocalDateTime receivedAt;
    private boolean processed;
    private final Map<String, Object> details;

    public DomainPaymentWebhookCorrelationEntity(
            Long id,
            String webhookId,
            DomainPaymentIdValue paymentId,
            LocalDateTime receivedAt,
            boolean processed,
            Map<String, Object> details) {
        this.id = id; // Can be null for new entities before persistence
        this.webhookId = Objects.requireNonNull(webhookId, "webhookId cannot be null");
        this.paymentId = paymentId; // Can be null initially until correlated
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");
        this.processed = processed;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
        
        validateBusinessRules();
    }

    /**
     * Static factory method to create a new WebhookCorrelation.
     */
    public static DomainPaymentWebhookCorrelationEntity create(
            String webhookId,
            Map<String, Object> details) {
        return new DomainPaymentWebhookCorrelationEntity(
                null, // ID will be assigned by persistence layer
                webhookId,
                null, // Payment ID will be set later during correlation
                LocalDateTime.now(),
                false,
                details
        );
    }

    /**
     * Correlates this webhook with a payment.
     * @param paymentId the payment ID to correlate with
     */
    public void correlateWithPayment(DomainPaymentIdValue paymentId) {
        Objects.requireNonNull(paymentId, "paymentId cannot be null");
        
        if (this.paymentId != null) {
            throw new IllegalStateException("Webhook is already correlated with payment: " + this.paymentId);
        }
        
        this.paymentId = paymentId;
    }

    /**
     * Marks this webhook as processed.
     */
    public void markProcessed() {
        if (processed) {
            return; // Already processed
        }
        
        if (paymentId == null) {
            throw new IllegalStateException("Cannot mark webhook as processed without payment correlation");
        }
        
        this.processed = true;
    }

    /**
     * Checks if this webhook has been processed.
     */
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Checks if this webhook is correlated with a payment.
     */
    public boolean isCorrelated() {
        return paymentId != null;
    }

    /**
     * Checks if this webhook is pending processing.
     */
    public boolean isPendingProcessing() {
        return isCorrelated() && !isProcessed();
    }

    /**
     * Gets a specific detail from the webhook payload.
     */
    public Object getDetail(String key) {
        return details.get(key);
    }

    /**
     * Adds or updates a detail in the webhook payload.
     */
    public void addDetail(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        details.put(key, value);
    }

    private void validateBusinessRules() {
        if (webhookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook ID cannot be empty");
        }
        
        if (processed && paymentId == null) {
            throw new IllegalArgumentException("Cannot have processed webhook without payment correlation");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public DomainPaymentIdValue getPaymentId() {
        return paymentId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public boolean getProcessed() {
        return processed;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentWebhookCorrelationEntity)) return false;
        DomainPaymentWebhookCorrelationEntity that = (DomainPaymentWebhookCorrelationEntity) o;
        // If both have IDs, compare by ID, otherwise compare by webhook ID
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return webhookId.equals(that.webhookId);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(webhookId);
    }

    @Override
    public String toString() {
        return "DomainPaymentWebhookCorrelationEntity{" +
                "id=" + id +
                ", webhookId='" + webhookId + '\'' +
                ", paymentId=" + paymentId +
                ", receivedAt=" + receivedAt +
                ", processed=" + processed +
                ", isCorrelated=" + isCorrelated() +
                '}';
    }
}