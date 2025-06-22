package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedEnumCorrelationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents correlation between a webhook and a payment record.
 */
@Entity
@Table(name = "payment_webhook_correlations")
public class DomainEntityPaymentWebhookCorrelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "correlation_status", nullable = false)
    private SharedEnumCorrelationStatus correlationStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor for JPA
    protected DomainEntityPaymentWebhookCorrelation() {}

    public DomainEntityPaymentWebhookCorrelation(UUID webhookId,
                                                UUID paymentId,
                                                SharedEnumCorrelationStatus correlationStatus,
                                                LocalDateTime createdAt) {
        this.webhookId = webhookId;
        this.paymentId = paymentId;
        this.correlationStatus = correlationStatus;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWebhookId() {
        return webhookId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public SharedEnumCorrelationStatus getCorrelationStatus() {
        return correlationStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}