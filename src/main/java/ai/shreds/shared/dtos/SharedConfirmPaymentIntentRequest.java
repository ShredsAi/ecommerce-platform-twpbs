package ai.shreds.shared.dtos;

import lombok.*;
import ai.shreds.application.dtos.ApplicationConfirmPaymentIntentDTO;

/**
 * DTO for confirming a payment intent request exposed by shared layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedConfirmPaymentIntentRequest {
    private String clientSecret;

    public ApplicationConfirmPaymentIntentDTO toApplicationDTO() {
        return ApplicationConfirmPaymentIntentDTO.builder()
                .clientSecret(clientSecret)
                .build();
    }
}