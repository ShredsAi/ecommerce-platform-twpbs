package ai.shreds.shared.value_objects;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedStockValidationRequestEvent {
    private String skuId;
    private String locationId;
    private BigDecimal requestedQuantity;
}