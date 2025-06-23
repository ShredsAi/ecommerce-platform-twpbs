package ai.shreds.application.dtos;

import lombok.*;
import java.util.UUID;
import java.util.Map;
import java.time.LocalDateTime;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;

/**
 * DTO for WebhookProcessed event data in the application layer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationWebhookProcessedDTO {

    private UUID paymentId;
    private String newStatus;
    private Map<String, Object> processorResponse;
    private LocalDateTime timestamp;
    
    /**
     * Map to domain update status command
     */
    public DomainUpdateStatusCommand toDomainCommand() {
        return new DomainUpdateStatusCommand(
            new DomainPaymentIdValue(paymentId),
            DomainPaymentStatusEnum.valueOf(newStatus),
            processorResponse
        );
    }
}