package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.List;
import ai.shreds.application.dtos.ApplicationOrderCreationRequestDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedCartCheckedOutEventDTO {
    private String customerId;
    private String cartId;
    private List<SharedCartItemDTO> items;
    private SharedAddressDTO billingAddress;
    private SharedAddressDTO shippingAddress;
    private SharedPaymentMethodDTO paymentMethod;
    private Instant timestamp;

    public ApplicationOrderCreationRequestDTO toApplicationDTO() {
        return ApplicationOrderCreationRequestDTO.fromSharedDTO(this);
    }
}
