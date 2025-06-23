package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedWebhookResponseDTO;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing the result of webhook processing within the application layer.
 */
@Data
@Builder
public class ApplicationWebhookResultDTO {
    private UUID webhookId;
    private SharedEnumWebhookProcessingStatus status;
    private ApplicationCorrelationResultDTO correlationResult;
    private LocalDateTime processedAt;

    /**
     * Map this application DTO to a shared response DTO for adapter layers.
     * @return SharedWebhookResponseDTO
     */
    public SharedWebhookResponseDTO toSharedResponse() {
        String message = null;
        if (correlationResult != null && correlationResult.getFailureReason() != null) {
            message = correlationResult.getFailureReason();
        }
        return SharedWebhookResponseDTO.builder()
                .status(status.name())
                .webhookId(webhookId)
                .message(message)
                .timestamp(processedAt)
                .build();
    }
}