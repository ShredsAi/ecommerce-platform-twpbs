package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedCartCheckoutEvent {
    private UUID cartId;
    private UUID customerId;
    private List<CartItemDTO> items;
}
