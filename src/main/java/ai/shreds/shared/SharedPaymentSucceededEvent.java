package ai.shreds.shared;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.domain.events.DomainPaymentSucceededEvent;

/**
 * Shared event DTO for successful payment events published to Kafka.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPaymentSucceededEvent {
    private UUID paymentId;
    private UUID orderId;
    private SharedMoneyValue amount;
    private LocalDateTime timestamp;

    public static SharedPaymentSucceededEvent fromDomainEvent(DomainPaymentSucceededEvent event) {
        return SharedPaymentSucceededEvent.builder()
                .paymentId(event.getPaymentId().getValue())
                .orderId(event.getOrderId().getValue())
                .amount(SharedMoneyValue.fromDomainValue(event.getAmount()))
                .timestamp(event.getTimestamp())
                .build();
    }
}