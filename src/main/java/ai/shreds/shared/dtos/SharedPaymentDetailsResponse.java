package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedProcessorResponseValue;
import ai.shreds.application.dtos.ApplicationPaymentDetailsDTO;

/**
 * Response DTO for payment details retrieval.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentDetailsResponse {
    private UUID id;
    private String status;
    private SharedMoneyValue amount;
    private UUID paymentIntentId;
    private SharedProcessorResponseValue processorResponse;
    private LocalDateTime processedAt;

    public static SharedPaymentDetailsResponse fromApplicationDTO(ApplicationPaymentDetailsDTO dto) {
        return SharedPaymentDetailsResponse.builder()
                .id(dto.getId())
                .status(dto.getStatus())
                .amount(dto.getAmount())
                .paymentIntentId(dto.getPaymentIntentId())
                .processorResponse(dto.getProcessorResponse())
                .processedAt(dto.getProcessedAt())
                .build();
    }
}