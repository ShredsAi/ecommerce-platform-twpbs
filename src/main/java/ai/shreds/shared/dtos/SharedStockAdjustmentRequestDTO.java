package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedStockAdjustmentRequestDTO {
    private String skuId;
    private String locationId;
    private BigDecimal adjustment;
    private SharedEnumAdjustmentReason reason;
}