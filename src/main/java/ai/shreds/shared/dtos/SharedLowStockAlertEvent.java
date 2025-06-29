package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedLowStockAlertEvent {
    private String alertId;
    private String skuId;
    private String locationId;
    private String alertLevel;
    private BigDecimal currentQuantity;
    private BigDecimal threshold;
}