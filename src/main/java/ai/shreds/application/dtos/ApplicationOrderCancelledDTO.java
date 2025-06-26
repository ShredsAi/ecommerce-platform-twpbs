package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.dtos.SharedOrderCancelledEventDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for application layer cancellation requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationOrderCancelledDTO {
    private UUID orderId;
    private String cancellationReason;
    private Boolean refundRequired;
    private Instant timestamp;

    /**
     * Convert this DTO to a domain cancellation request entity.
     */
    public DomainCancellationRequestEntity toDomainCancellation() {
        return DomainCancellationRequestEntity.fromApplicationDTO(this);
    }

    /**
     * Create an application DTO from a shared cancellation event.
     */
    public static ApplicationOrderCancelledDTO fromSharedDTO(SharedOrderCancelledEventDTO shared) {
        return new ApplicationOrderCancelledDTO(
                shared.getOrderId(),
                shared.getCancellationReason(),
                shared.getRefundRequired(),
                shared.getTimestamp()
        );
    }
}
