package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import ai.shreds.shared.enums.SharedErrorTypeEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderCreationFailedEventDTO {
    private String cartId;
    private String customerId;
    private SharedErrorTypeEnum errorType;
    private String errorMessage;
    private String failureReason;
    private Instant timestamp;
    private String correlationId;
    private String eventId;
    private Instant occurredOn;
}