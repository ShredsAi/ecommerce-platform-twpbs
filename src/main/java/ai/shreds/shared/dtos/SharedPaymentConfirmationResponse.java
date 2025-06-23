package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Map;
import ai.shreds.application.dtos.ApplicationPaymentConfirmationDTO;

/**
 * Response DTO for payment intent confirmation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentConfirmationResponse {
    private UUID id;
    private String status;
    private UUID paymentId;
    private LocalDateTime processedAt;
    private Boolean requiresAction;
    private Map<String, Object> nextAction;

    public static SharedPaymentConfirmationResponse fromApplicationDTO(ApplicationPaymentConfirmationDTO dto) {
        return SharedPaymentConfirmationResponse.builder()
                .id(dto.getId())
                .status(dto.getStatus())
                .paymentId(dto.getPaymentId())
                .processedAt(dto.getProcessedAt())
                .requiresAction(dto.getRequiresAction())
                .nextAction(dto.getNextAction())
                .build();
    }
}