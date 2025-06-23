package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Map;
import ai.shreds.application.dtos.ApplicationWebhookProcessedDTO;

/**
 * Event DTO for processed payment webhook from shared database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentWebhookProcessedEvent {
    private UUID paymentId;
    private String newStatus;
    private Map<String, Object> processorResponse;
    private LocalDateTime timestamp;

    public ApplicationWebhookProcessedDTO toApplicationDTO() {
        return ApplicationWebhookProcessedDTO.builder()
                .paymentId(paymentId)
                .newStatus(newStatus)
                .processorResponse(processorResponse)
                .timestamp(timestamp)
                .build();
    }
}