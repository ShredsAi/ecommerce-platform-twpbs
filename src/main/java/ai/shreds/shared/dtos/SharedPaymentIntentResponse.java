package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.application.dtos.ApplicationPaymentIntentDTO;

/**
 * Response DTO for payment intent creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentIntentResponse {
    private UUID id;
    private String clientSecret;
    private String status;
    private LocalDateTime expiresAt;

    public static SharedPaymentIntentResponse fromApplicationDTO(ApplicationPaymentIntentDTO dto) {
        return SharedPaymentIntentResponse.builder()
                .id(dto.getId())
                .clientSecret(dto.getClientSecret())
                .status(dto.getStatus())
                .expiresAt(dto.getExpiresAt())
                .build();
    }
}