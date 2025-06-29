package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedStockAdjustmentRequestDTO;
import ai.shreds.shared.dtos.SharedStockAdjustmentResponseDTO;

public interface ApplicationStockAdjustmentInputPort {
    /**
     * Adjusts the stock quantity for a specific SKU at a specific location
     *
     * @param request The adjustment request containing SKU, location, quantity and reason
     * @return Response containing the adjustment result
     */
    SharedStockAdjustmentResponseDTO adjustStock(SharedStockAdjustmentRequestDTO request);
}