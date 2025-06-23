package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentWebhookCorrelationEntity;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_webhook_correlations")
public class InfrastructurePaymentWebhookJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "webhook_id", nullable = false, unique = true, length = 128)
    private String webhookId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (processed == null) {
            processed = false;
        }
    }

    public static InfrastructurePaymentWebhookJpaEntity fromDomainEntity(DomainPaymentWebhookCorrelationEntity domain) {
        InfrastructurePaymentWebhookJpaEntity jpa = new InfrastructurePaymentWebhookJpaEntity();
        jpa.setId(domain.getId());
        jpa.setWebhookId(domain.getWebhookId());
        jpa.setPaymentId(domain.getPaymentId() != null ? domain.getPaymentId().getValue() : null);
        jpa.setReceivedAt(domain.getReceivedAt());
        jpa.setProcessed(domain.isProcessed());
        
        // Serialize webhook details to JSON
        if (domain.getDetails() != null && !domain.getDetails().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                jpa.setDetails(mapper.writeValueAsString(domain.getDetails()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                jpa.setDetails("{}");
            }
        }
        
        return jpa;
    }

    public DomainPaymentWebhookCorrelationEntity toDomainEntity() {
        Map<String, Object> webhookDetails = null;

        if (this.details != null && !this.details.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                webhookDetails = mapper.readValue(
                    this.details,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                // Create empty map on parsing error
                webhookDetails = Map.of();
            }
        } else {
            webhookDetails = Map.of();
        }

        return new DomainPaymentWebhookCorrelationEntity(
            this.id,
            this.webhookId,
            this.paymentId != null ? new DomainPaymentIdValue(this.paymentId) : null,
            this.receivedAt,
            this.processed,
            webhookDetails
        );
    }
}
