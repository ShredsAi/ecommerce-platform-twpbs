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
 * Error response DTO for webhook processing failures.
 * Used by exception handlers to return structured error information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWebhookErrorResponseDTO {
    
    @NotBlank(message = "Error message is required")
    private String error;
    
    private String details;
    
    private UUID webhookId;
    
    @NotNull(message = "Timestamp is required")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static SharedWebhookErrorResponseDTO validationError(String error, String details, UUID webhookId) {
        return SharedWebhookErrorResponseDTO.builder()
                .error(error)
                .details(details)
                .webhookId(webhookId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SharedWebhookErrorResponseDTO processingError(String error, String details, UUID webhookId) {
        return SharedWebhookErrorResponseDTO.builder()
                .error(error)
                .details(details)
                .webhookId(webhookId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SharedWebhookErrorResponseDTO genericError(String error, String details) {
        return SharedWebhookErrorResponseDTO.builder()
                .error(error)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SharedWebhookErrorResponseDTO notFound(UUID webhookId) {
        return SharedWebhookErrorResponseDTO.builder()
                .error("Webhook not found")
                .details("No webhook found with the provided identifier")
                .webhookId(webhookId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SharedWebhookErrorResponseDTO unauthorized(String reason, UUID webhookId) {
        return SharedWebhookErrorResponseDTO.builder()
                .error("Unauthorized")
                .details(reason != null ? reason : "Invalid signature or authentication failed")
                .webhookId(webhookId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SharedWebhookErrorResponseDTO internalError(String details, UUID webhookId) {
        return SharedWebhookErrorResponseDTO.builder()
                .error("Internal server error")
                .details(details != null ? details : "An unexpected error occurred while processing the webhook")
                .webhookId(webhookId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
