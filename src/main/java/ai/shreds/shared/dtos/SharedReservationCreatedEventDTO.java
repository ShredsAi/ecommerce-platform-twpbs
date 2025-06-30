package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for reservation created event published to Kafka.
 * Notifies Cart Service that reservation was successfully created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedReservationCreatedEventDTO {
    private String reservationId;
    private String cartId;
    private String status;
    private String expiresAt; // ISO-8601 format
}
