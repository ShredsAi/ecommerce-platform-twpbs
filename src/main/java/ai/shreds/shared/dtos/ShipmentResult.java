package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO representing the result of a shipment operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String trackingNumber;
    private String carrier;
    private Date estimatedDeliveryDate;
    private String errorMessage;
    
    /**
     * Checks if the shipment operation was successful.
     * 
     * @return true if the operation succeeded
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * Checks if the shipment operation failed.
     * 
     * @return true if the operation failed
     */
    public boolean isFailed() {
        return !isSuccess();
    }
    
    /**
     * Checks if an error message is present.
     * 
     * @return true if error message is present and not empty
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    /**
     * Checks if tracking information is available.
     * 
     * @return true if tracking number and carrier are present
     */
    public boolean hasTrackingInfo() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty() &&
               carrier != null && !carrier.trim().isEmpty();
    }
    
    /**
     * Factory method to create a successful shipment result.
     * 
     * @param trackingNumber the tracking number
     * @param carrier the shipping carrier
     * @param estimatedDeliveryDate the estimated delivery date
     * @return a new successful ShipmentResult
     */
    public static ShipmentResult success(String trackingNumber, String carrier, Date estimatedDeliveryDate) {
        return ShipmentResult.builder()
                .success(true)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .build();
    }
    
    /**
     * Factory method to create a successful shipment result without estimated delivery date.
     * 
     * @param trackingNumber the tracking number
     * @param carrier the shipping carrier
     * @return a new successful ShipmentResult without delivery date
     */
    public static ShipmentResult success(String trackingNumber, String carrier) {
        return ShipmentResult.builder()
                .success(true)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .build();
    }
    
    /**
     * Factory method to create a failed shipment result.
     * 
     * @param errorMessage the error message
     * @return a new failed ShipmentResult
     */
    public static ShipmentResult failed(String errorMessage) {
        return ShipmentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Factory method to create a failed shipment result with partial information.
     * 
     * @param trackingNumber the tracking number if available
     * @param errorMessage the error message
     * @return a new failed ShipmentResult with tracking number
     */
    public static ShipmentResult failed(String trackingNumber, String errorMessage) {
        return ShipmentResult.builder()
                .success(false)
                .trackingNumber(trackingNumber)
                .errorMessage(errorMessage)
                .build();
    }
}