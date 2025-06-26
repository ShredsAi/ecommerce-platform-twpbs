package ai.shreds.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing the result of a cancellation handling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCancellationResultDTO {
    private UUID orderId;
    private String cancellationStatus;
    private String refundStatus;
    private String message;
    private Instant completedAt;
}
