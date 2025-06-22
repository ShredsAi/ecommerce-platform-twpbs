package ai.shreds.domain.entities;

import ai.shreds.application.dtos.ApplicationWebhookStatusDTO;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a payment processor webhook persisted for reconciliation.
 */
@Entity
@Table(name = "payment_webhooks", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"external_event_id", "processor_type"}))
public class DomainEntityPaymentWebhook {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processor_type", nullable = false)
    private SharedEnumPaymentProcessorType processorType;
    
    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
    
    @Column(name = "signature")
    private String signature;
    
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private SharedEnumWebhookProcessingStatus processingStatus;
    
    @Column(name = "payment_id")
    private UUID paymentId;
    
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    protected DomainEntityPaymentWebhook() {}

    public DomainEntityPaymentWebhook(
            UUID id,
            SharedEnumPaymentProcessorType processorType,
            String externalEventId,
            String eventType,
            String rawPayload,
            String signature,
            LocalDateTime receivedAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.processorType = processorType;
        this.externalEventId = externalEventId;
        this.eventType = eventType;
        this.rawPayload = rawPayload;
        this.signature = signature;
        this.isVerified = false;
        this.processingStatus = SharedEnumWebhookProcessingStatus.PENDING;
        this.paymentId = null;
        this.receivedAt = receivedAt;
        this.processedAt = null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public SharedEnumPaymentProcessorType getProcessorType() {
        return processorType;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public SharedEnumWebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
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

    public void markAsVerified() {
        this.isVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsProcessed() {
        this.processingStatus = SharedEnumWebhookProcessingStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = this.processedAt;
    }

    public void markAsFailed() {
        this.processingStatus = SharedEnumWebhookProcessingStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = this.processedAt;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
        this.updatedAt = LocalDateTime.now();
    }

    public ApplicationWebhookStatusDTO toApplicationDTO() {
        return ApplicationWebhookStatusDTO.builder()
                .webhookId(this.id)
                .processingStatus(this.processingStatus)
                .paymentId(this.paymentId)
                .receivedAt(this.receivedAt)
                .processedAt(this.processedAt)
                .eventType(this.eventType)
                .processorType(this.processorType)
                .build();
    }
}