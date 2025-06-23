package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Standard response DTO for webhook processing operations.
 * Used to communicate the result of webhook processing back to payment processors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWebhookResponseDTO {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotNull(message = "Webhook ID is required")
    private UUID webhookId;
    
    private String message;
    
    @NotNull(message = "Timestamp is required")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Factory method to create a successful response.
     *
     * @param webhookId The unique identifier of the processed webhook
     * @return SharedWebhookResponseDTO indicating successful processing
     */
    public static SharedWebhookResponseDTO success(UUID webhookId) {
        return SharedWebhookResponseDTO.builder()
                .status("processed")
                .webhookId(webhookId)
                .message("Webhook processed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method to create a successful response with custom message.
     *
     * @param webhookId The unique identifier of the processed webhook
     * @param message   Custom success message
     * @return SharedWebhookResponseDTO indicating successful processing
     */
    public static SharedWebhookResponseDTO success(UUID webhookId, String message) {
        return SharedWebhookResponseDTO.builder()
                .status("processed")
                .webhookId(webhookId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method to create a duplicate response (webhook already processed).
     *
     * @param webhookId The unique identifier of the duplicate webhook
     * @return SharedWebhookResponseDTO indicating duplicate processing
     */
    public static SharedWebhookResponseDTO duplicate(UUID webhookId) {
        return SharedWebhookResponseDTO.builder()
                .status("duplicate")
                .webhookId(webhookId)
                .message("Webhook already processed")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method to create a failed processing response.
     *
     * @param webhookId The unique identifier of the webhook that failed
     * @param reason    The reason for processing failure
     * @return SharedWebhookResponseDTO indicating processing failure
     */
    public static SharedWebhookResponseDTO failed(UUID webhookId, String reason) {
        return SharedWebhookResponseDTO.builder()
                .status("failed")
                .webhookId(webhookId)
                .message(reason != null ? reason : "Webhook processing failed")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method to create a validation error response.
     *
     * @param webhookId The unique identifier of the webhook with validation issues
     * @param reason    The validation error reason
     * @return SharedWebhookResponseDTO indicating validation failure
     */
    public static SharedWebhookResponseDTO validationError(UUID webhookId, String reason) {
        return SharedWebhookResponseDTO.builder()
                .status("validation_error")
                .webhookId(webhookId)
                .message(reason != null ? reason : "Webhook validation failed")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Checks if this response represents a successful processing result.
     *
     * @return true if status indicates success, false otherwise
     */
    public boolean isSuccess() {
        return "processed".equals(status) || "duplicate".equals(status);
    }

    /**
     * Checks if this response represents a processing failure.
     *
     * @return true if status indicates failure, false otherwise
     */
    public boolean isFailure() {
        return "failed".equals(status) || "validation_error".equals(status);
    }
}