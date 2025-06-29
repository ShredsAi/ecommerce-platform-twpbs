package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedStockLevelDTO {
    private String skuId;
    private String locationId;
    private BigDecimal quantity;
    private BigDecimal reserved;
    private BigDecimal available;
}