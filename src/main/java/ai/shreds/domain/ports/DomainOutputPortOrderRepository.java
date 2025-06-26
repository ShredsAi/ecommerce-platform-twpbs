package ai.shreds.domain.ports;

import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;

/**
 * Port for order data operations in the domain layer.
 */
public interface DomainOutputPortOrderRepository {

    /**
     * Retrieves an order snapshot by its ID.
     *
     * @param orderId the unique identifier of the order
     * @return the snapshot of the order
     */
    SharedOrderSnapshotDTO findOrderSnapshot(String orderId);

    /**
     * Updates the status of an order.
     *
     * @param orderId the unique identifier of the order
     * @param newStatus the new status value
     */
    void updateOrderStatus(String orderId, String newStatus);
}