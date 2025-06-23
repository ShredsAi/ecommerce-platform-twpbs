package ai.shreds.shared.dtos;

import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ai.shreds.application.dtos.ApplicationOrderPlacedDTO;

/**
 * Event DTO for order placed consumed from external Kafka.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedOrderPlacedEvent {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private LocalDateTime timestamp;

    public ApplicationOrderPlacedDTO toApplicationDTO() {
        return ApplicationOrderPlacedDTO.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .timestamp(timestamp)
                .build();
    }
}