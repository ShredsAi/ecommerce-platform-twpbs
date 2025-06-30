package ai.shreds.domain.ports;

import ai.shreds.shared.dtos.ERPAdjustmentItem;
import java.util.List;

public interface DomainInputPortERPSync {
    void reconcileStock(String batchId, List<ERPAdjustmentItem> adjustments);
}