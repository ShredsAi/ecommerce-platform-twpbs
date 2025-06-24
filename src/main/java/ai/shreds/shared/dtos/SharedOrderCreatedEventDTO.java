package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import ai.shreds.shared.enums.SharedOrderStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedOrderCreatedEventDTO {
    private String orderId;
    private String orderNumber;
    private String customerId;
    private SharedOrderStatusEnum orderStatus;
    private SharedMoneyDTO totalAmount;
    private Integer itemCount;
    private Instant timestamp;
    private String correlationId;
    private String eventId;
    private Instant occurredOn;
}