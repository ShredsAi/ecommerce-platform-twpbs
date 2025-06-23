package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.application.dtos.ApplicationCreatePaymentIntentDTO;

/**
 * DTO for creating a payment intent request exposed by shared layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedCreatePaymentIntentRequest {
    private UUID orderId;
    private UUID customerId;
    private SharedMoneyValue amount;
    private UUID paymentMethodId;

    public ApplicationCreatePaymentIntentDTO toApplicationDTO() {
        return ApplicationCreatePaymentIntentDTO.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .paymentMethodId(paymentMethodId)
                .build();
    }
}