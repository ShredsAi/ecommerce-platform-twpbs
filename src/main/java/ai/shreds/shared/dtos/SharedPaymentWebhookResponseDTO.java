package ai.shreds.shared.dtos;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ai.shreds.application.dtos.ApplicationPaymentWebhookResultDTO;

/**
 * Response DTO for payment webhook callbacks shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentWebhookResponseDTO {

    @NotBlank(message = "Status is required")
    private String status;

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private String nextStep;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    /**
     * Creates a shared response DTO from application layer result DTO.
     */
    public static SharedPaymentWebhookResponseDTO fromApplicationDTO(ApplicationPaymentWebhookResultDTO dto) {
        return SharedPaymentWebhookResponseDTO.builder()
                .status(dto.getStatus())
                .orderId(dto.getOrderId())
                .nextStep(dto.getNextStep())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public static SharedPaymentWebhookResponseDTO success(UUID orderId, String nextStep) {
        return SharedPaymentWebhookResponseDTO.builder()
                .status("SUCCESS")
                .orderId(orderId)
                .nextStep(nextStep)
                .timestamp(Instant.now())
                .build();
    }

    public static SharedPaymentWebhookResponseDTO failure(UUID orderId, String errorReason) {
        return SharedPaymentWebhookResponseDTO.builder()
                .status("FAILED")
                .orderId(orderId)
                .nextStep(errorReason)
                .timestamp(Instant.now())
                .build();
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public void validate() {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }
}