package ai.shreds.application.dtos;

import lombok.*;
import java.util.UUID;
import ai.shreds.domain.commands.DomainConfirmIntentCommand;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;

/**
 * DTO for confirming a payment intent in the application layer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationConfirmPaymentIntentDTO {

    private String clientSecret;

    /**
     * Map to domain confirm command
     */
    public DomainConfirmIntentCommand toDomainCommand(UUID intentId) {
        return new DomainConfirmIntentCommand(
            new DomainPaymentIntentIdValue(intentId),
            clientSecret
        );
    }
}