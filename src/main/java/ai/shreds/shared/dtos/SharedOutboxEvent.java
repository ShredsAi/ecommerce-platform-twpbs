package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedOutboxEvent {
    private String eventId;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    private String payload;
    private Instant occurredOn;
    private Boolean processed;
    private Instant processedOn;
}