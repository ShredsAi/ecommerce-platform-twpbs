package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderItemEntity;
import java.util.List;
import java.util.UUID;

/**
 * Domain output port for inventory service operations.
 * This port defines the contract for inventory management operations.
 */
public interface DomainOutputPortInventoryService {

    /**
     * Allocates inventory items for an order.
     * @param orderId The order ID to allocate items for
     * @param items The list of order items to allocate
     * @return true if allocation was successful, false otherwise
     * @throws ai.shreds.domain.exceptions.DomainSagaException if allocation fails
     */
    boolean allocateItems(UUID orderId, List<DomainOrderItemEntity> items);

    /**
     * Releases previously allocated inventory items.
     * @param orderId The order ID to release items for
     * @param items The list of order items to release
     * @return true if release was successful, false otherwise
     * @throws ai.shreds.domain.exceptions.DomainSagaException if release fails
     */
    boolean releaseItems(UUID orderId, List<DomainOrderItemEntity> items);

    /**
     * Checks if all items are available for allocation.
     * @param items The list of order items to check
     * @return true if all items are available, false otherwise
     */
    boolean checkAvailability(List<DomainOrderItemEntity> items);

    /**
     * Reserves inventory items temporarily.
     * @param orderId The order ID to reserve items for
     * @param items The list of order items to reserve
     * @return true if reservation was successful, false otherwise
     */
    boolean reserveItems(UUID orderId, List<DomainOrderItemEntity> items);

    /**
     * Cancels a temporary reservation.
     * @param orderId The order ID to cancel reservation for
     * @return true if cancellation was successful, false otherwise
     */
    boolean cancelReservation(UUID orderId);
}