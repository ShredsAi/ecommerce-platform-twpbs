package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedERPUpdateMessage {
    private String erpBatchId;
    private List<ERPAdjustmentItem> adjustments;
}