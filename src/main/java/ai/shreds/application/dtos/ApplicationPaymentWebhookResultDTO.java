package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedPaymentWebhookResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Result DTO for processing a payment webhook in application layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationPaymentWebhookResultDTO {
    private String status;
    private UUID orderId;
    private String nextStep;
    private Instant timestamp;

    /**
     * Convert to shared response DTO for forwarding back to payment adapter.
     */
    public SharedPaymentWebhookResponseDTO toSharedDTO() {
        return SharedPaymentWebhookResponseDTO.fromApplicationDTO(this);
    }
}
