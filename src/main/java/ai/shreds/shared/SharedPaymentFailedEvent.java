package ai.shreds.shared;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.domain.events.DomainPaymentFailedEvent;

/**
 * Shared event DTO for failed payment events published to Kafka.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentFailedEvent {
    private UUID paymentId;
    private UUID orderId;
    private String failureReason;
    private LocalDateTime timestamp;

    public static SharedPaymentFailedEvent fromDomainEvent(DomainPaymentFailedEvent event) {
        return SharedPaymentFailedEvent.builder()
                .paymentId(event.getPaymentId().getValue())
                .orderId(event.getOrderId().getValue())
                .failureReason(event.getFailureReason())
                .timestamp(event.getTimestamp())
                .build();
    }
}