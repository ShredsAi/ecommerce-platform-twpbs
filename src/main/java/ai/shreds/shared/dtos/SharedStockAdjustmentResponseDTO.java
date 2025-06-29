package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedStockAdjustmentResponseDTO {
    private String status;
    private String ledgerId;
    private BigDecimal newQuantity;
    private Instant timestamp;
}