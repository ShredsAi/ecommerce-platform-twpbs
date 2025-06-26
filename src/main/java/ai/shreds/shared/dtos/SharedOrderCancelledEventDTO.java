package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.time.Instant;
import ai.shreds.application.dtos.ApplicationOrderCancelledDTO;

/**
 * DTO representing an order cancelled event shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderCancelledEventDTO {

    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
    
    @NotNull(message = "Refund required flag is required")
    private Boolean refundRequired;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    /**
     * Converts this shared DTO to application layer DTO.
     *
     * @return ApplicationOrderCancelledDTO with the same data
     */
    public ApplicationOrderCancelledDTO toApplicationDTO() {
        return ApplicationOrderCancelledDTO.fromSharedDTO(this);
    }
    
    /**
     * Validates the order cancellation event data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        if (refundRequired == null) {
            throw new IllegalArgumentException("Refund required flag is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }
    
    /**
     * Checks if a refund is required for this cancellation.
     * 
     * @return true if refund is required
     */
    public boolean requiresRefund() {
        return Boolean.TRUE.equals(refundRequired);
    }
    
    /**
     * Checks if the cancellation happened recently (within last hour).
     * 
     * @return true if cancellation is recent
     */
    public boolean isRecentCancellation() {
        if (timestamp == null) {
            return false;
        }
        return timestamp.isAfter(Instant.now().minusSeconds(3600)); // 1 hour ago
    }
}
