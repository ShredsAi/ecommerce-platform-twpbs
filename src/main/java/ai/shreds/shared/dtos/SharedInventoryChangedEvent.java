package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedInventoryChangedEvent {
    private String eventType;
    private String skuId;
    private String locationId;
    private BigDecimal previousQuantity;
    private BigDecimal newQuantity;
    private String source;
}