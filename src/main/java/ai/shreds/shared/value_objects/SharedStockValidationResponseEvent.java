package ai.shreds.shared.value_objects;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedStockValidationResponseEvent {
    private String skuId;
    private String locationId;
    private Boolean isAvailable;
    private BigDecimal availableQuantity;
    private BigDecimal requestedQuantity;
}
