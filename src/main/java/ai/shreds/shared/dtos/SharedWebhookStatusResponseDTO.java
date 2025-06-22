package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO containing the current status and details of a webhook processing request.
 * Used by the webhook status inquiry API to provide visibility into processing status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWebhookStatusResponseDTO {
    
    @NotNull(message = "Webhook ID is required")
    private UUID webhookId;
    
    @NotNull(message = "Processing status is required")
    private SharedEnumWebhookProcessingStatus processingStatus;
    
    private UUID paymentId;
    
    @NotNull(message = "Received timestamp is required")
    private LocalDateTime receivedAt;
    
    private LocalDateTime processedAt;
    
    private String eventType;
    
    @NotNull(message = "Processor type is required")
    private SharedEnumPaymentProcessorType processorType;

    public static SharedWebhookStatusResponseDTO pending(UUID webhookId,
                                                         SharedEnumPaymentProcessorType processorType,
                                                         String eventType,
                                                         LocalDateTime receivedAt) {
        return SharedWebhookStatusResponseDTO.builder()
                .webhookId(webhookId)
                .processingStatus(SharedEnumWebhookProcessingStatus.PENDING)
                .processorType(processorType)
                .eventType(eventType)
                .receivedAt(receivedAt)
                .build();
    }

    public static SharedWebhookStatusResponseDTO processed(UUID webhookId,
                                                           SharedEnumPaymentProcessorType processorType,
                                                           String eventType,
                                                           LocalDateTime receivedAt,
                                                           LocalDateTime processedAt,
                                                           UUID paymentId) {
        return SharedWebhookStatusResponseDTO.builder()
                .webhookId(webhookId)
                .processingStatus(SharedEnumWebhookProcessingStatus.PROCESSED)
                .processorType(processorType)
                .eventType(eventType)
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .paymentId(paymentId)
                .build();
    }

    public static SharedWebhookStatusResponseDTO failed(UUID webhookId,
                                                        SharedEnumPaymentProcessorType processorType,
                                                        String eventType,
                                                        LocalDateTime receivedAt,
                                                        LocalDateTime processedAt) {
        return SharedWebhookStatusResponseDTO.builder()
                .webhookId(webhookId)
                .processingStatus(SharedEnumWebhookProcessingStatus.FAILED)
                .processorType(processorType)
                .eventType(eventType)
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .build();
    }

    public static SharedWebhookStatusResponseDTO ignored(UUID webhookId,
                                                         SharedEnumPaymentProcessorType processorType,
                                                         String eventType,
                                                         LocalDateTime receivedAt,
                                                         LocalDateTime processedAt) {
        return SharedWebhookStatusResponseDTO.builder()
                .webhookId(webhookId)
                .processingStatus(SharedEnumWebhookProcessingStatus.IGNORED)
                .processorType(processorType)
                .eventType(eventType)
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .build();
    }

    public boolean isPending() {
        return SharedEnumWebhookProcessingStatus.PENDING.equals(processingStatus);
    }

    public boolean isProcessed() {
        return SharedEnumWebhookProcessingStatus.PROCESSED.equals(processingStatus);
    }

    public boolean isFailed() {
        return SharedEnumWebhookProcessingStatus.FAILED.equals(processingStatus);
    }

    public boolean isIgnored() {
        return SharedEnumWebhookProcessingStatus.IGNORED.equals(processingStatus);
    }

    public boolean isFinished() {
        return !isPending();
    }

    public String getStatusDescription() {
        switch (processingStatus) {
            case PENDING:
                return "Webhook received and queued for processing";
            case PROCESSED:
                return paymentId != null
                    ? String.format("Webhook processed successfully and correlated with payment %s", paymentId)
                    : "Webhook processed successfully";
            case FAILED:
                return "Webhook processing failed";
            case IGNORED:
                return "Webhook ignored (duplicate or irrelevant event)";
            default:
                return "Unknown status";
        }
    }
}
