package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;
import ai.shreds.application.dtos.ApplicationShippingUpdateDTO;

/**
 * DTO representing a shipping status update shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedShippingUpdateDTO {
    
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private Date estimatedDeliveryDate;
    
    private Date actualDeliveryDate;
    
    @NotBlank(message = "Carrier is required")
    private String carrier;
    
    /**
     * Converts this shared DTO to application layer DTO.
     *
     * @return ApplicationShippingUpdateDTO with the same data
     */
    public ApplicationShippingUpdateDTO toApplicationDTO() {
        return ApplicationShippingUpdateDTO.fromSharedDTO(this);
    }
    
    /**
     * Validates the shipping update data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Tracking number is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (carrier == null || carrier.trim().isEmpty()) {
            throw new IllegalArgumentException("Carrier is required");
        }
    }
    
    /**
     * Checks if the shipment has been delivered.
     * 
     * @return true if status indicates delivery
     */
    public boolean isDelivered() {
        return "DELIVERED".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if the shipment is in transit.
     * 
     * @return true if status indicates shipment is shipped
     */
    public boolean isShipped() {
        return "SHIPPED".equalsIgnoreCase(status) || "IN_TRANSIT".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if the shipping has failed.
     * 
     * @return true if status indicates failure
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if delivery date is overdue (estimated date has passed).
     * 
     * @return true if estimated delivery date has passed and not yet delivered
     */
    public boolean isOverdue() {
        return estimatedDeliveryDate != null &&
               estimatedDeliveryDate.before(new Date()) &&
               !isDelivered();
    }
}
