package ai.shreds.application.dtos;

import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * DTO for OrderPlaced event data in the application layer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationOrderPlacedDTO {

    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    
    /**
     * Map to domain create intent command
     * Note: This is a simplified version that assumes default currency (USD) and
     * no payment method (as it will be added later in the flow)
     */
    public DomainCreateIntentCommand toDomainCommand() {
        // Create a money value with USD as default currency
        DomainMoneyValue moneyValue = new DomainMoneyValue(amount, "USD");
        
        // Create the command without a payment method ID (will be handled separately)
        return new DomainCreateIntentCommand(
            new DomainOrderIdValue(orderId),
            new DomainCustomerIdValue(customerId),
            moneyValue,
            null // No payment method ID at this point
        );
    }
}