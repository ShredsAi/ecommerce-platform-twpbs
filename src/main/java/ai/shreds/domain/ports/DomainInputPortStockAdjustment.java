package ai.shreds.domain.ports;

import ai.shreds.shared.dtos.SharedStockAdjustmentResponseDTO;
import ai.shreds.domain.value_objects.DomainValueQuantityAdjustment;

public interface DomainInputPortStockAdjustment {
    SharedStockAdjustmentResponseDTO adjustStock(String skuId, String locationId, DomainValueQuantityAdjustment adjustment);
}