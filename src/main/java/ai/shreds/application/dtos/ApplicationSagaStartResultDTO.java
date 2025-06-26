package ai.shreds.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Result DTO for starting a saga.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSagaStartResultDTO {
    private UUID sagaId;
    private UUID orderId;
    private String status;
    private String nextStep;
    private Instant createdAt;
}
