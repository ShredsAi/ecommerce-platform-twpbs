package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.shared.dtos.SharedShippingUpdateDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * DTO for receiving shipping update events in the application layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationShippingUpdateDTO {
    private String trackingNumber;
    private UUID orderId;
    private String status;
    private Date estimatedDeliveryDate;
    private Date actualDeliveryDate;
    private String carrier;

    /**
     * Map this DTO to a domain shipping details entity.
     */
    public DomainShippingDetailsEntity toDomainShipping() {
        return DomainShippingDetailsEntity.fromApplicationDTO(this);
    }

    /**
     * Build an application DTO from shared shipping update DTO.
     */
    public static ApplicationShippingUpdateDTO fromSharedDTO(SharedShippingUpdateDTO shared) {
        return new ApplicationShippingUpdateDTO(
            shared.getTrackingNumber(),
            shared.getOrderId(),
            shared.getStatus(),
            shared.getEstimatedDeliveryDate(),
            shared.getActualDeliveryDate(),
            shared.getCarrier()
        );
    }
}
