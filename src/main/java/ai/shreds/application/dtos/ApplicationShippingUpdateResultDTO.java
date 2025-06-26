package ai.shreds.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Result DTO returned after processing a shipping update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationShippingUpdateResultDTO {
    private UUID orderId;
    private String updatedStatus;
    private Boolean notificationSent;
    private Instant processedAt;
}
