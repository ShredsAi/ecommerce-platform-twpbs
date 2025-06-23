package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedWebhookStatusResponseDTO;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationWebhookStatusDTO {
    private UUID webhookId;
    private SharedEnumWebhookProcessingStatus processingStatus;
    private UUID paymentId;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String eventType;
    private SharedEnumPaymentProcessorType processorType;

    public SharedWebhookStatusResponseDTO toSharedStatusResponse() {
        return SharedWebhookStatusResponseDTO.builder()
                .webhookId(webhookId)
                .processingStatus(processingStatus)
                .paymentId(paymentId)
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .eventType(eventType)
                .processorType(processorType)
                .build();
    }
}
