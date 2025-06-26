package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing an order creation event in the application layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationOrderCreatedDTO {
    private UUID orderId;
    private String status;
    private SharedMoneyValue totalAmount;
    private String customerId;
    private List<SharedOrderItemDTO> items;
    private SharedAddressValue billingAddress;
    private SharedAddressValue shippingAddress;

    /**
     * Convert this application DTO to a domain Order entity.
     */
    public DomainOrderEntity toDomainOrder() {
        return DomainOrderEntity.fromApplicationDTO(this);
    }

    /**
     * Build an application DTO from a shared order-created event.
     */
    public static ApplicationOrderCreatedDTO fromSharedDTO(SharedOrderCreatedEventDTO shared) {
        return new ApplicationOrderCreatedDTO(
            shared.getOrderId(),
            shared.getStatus(),
            shared.getTotalAmount(),
            shared.getCustomerId(),
            shared.getItems(),
            shared.getBillingAddress(),
            shared.getShippingAddress()
        );
    }
}
