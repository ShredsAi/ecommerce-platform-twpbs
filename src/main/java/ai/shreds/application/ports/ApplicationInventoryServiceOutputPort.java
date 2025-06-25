package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedReturnRequestDTO;

/**
 * Output port for inventory service interactions.
 */
public interface ApplicationInventoryServiceOutputPort {

    /**
     * Release reserved stock for a cancelled order.
     * @param orderSnapshot snapshot of the order
     */
    void releaseReservedStock(SharedOrderSnapshotDTO orderSnapshot);

    /**
     * Increment stock for a processed return.
     * @param returnRequest return request DTO
     */
    void incrementStock(SharedReturnRequestDTO returnRequest);

    /**
     * Check if stock is available for an order.
     * @param orderId identifier of the order
     * @return true if stock is available
     */
    boolean checkStockAvailability(String orderId);
}
