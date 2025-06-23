package ai.shreds.application.dtos;

import lombok.*;
import java.util.UUID;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainPaymentMethodIdValue;

/**
 * DTO for creating a payment intent in the application layer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCreatePaymentIntentDTO {

    private UUID orderId;
    private UUID customerId;
    private SharedMoneyValue amount;
    private UUID paymentMethodId;

    /**
     * Map to domain command
     */
    public DomainCreateIntentCommand toDomainCommand() {
        return new DomainCreateIntentCommand(
            new DomainOrderIdValue(orderId),
            new DomainCustomerIdValue(customerId),
            amount.toDomainValue(),
            new DomainPaymentMethodIdValue(paymentMethodId)
        );
    }
}