package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain output port for order repository operations.
 * This port defines the contract for order persistence operations.
 */
public interface DomainOutputPortOrderRepository {

    /**
     * Finds an order by its ID.
     * @param orderId The order ID to search for
     * @return Optional containing the order if found, empty otherwise
     * @throws ai.shreds.domain.exceptions.DomainOrderNotFoundException if order not found and required
     */
    Optional<DomainOrderEntity> findById(UUID orderId);

    /**
     * Saves an order entity.
     * @param order The order entity to save
     * @return The saved order entity
     * @throws ai.shreds.domain.exceptions.DomainValidationException if order data is invalid
     */
    DomainOrderEntity save(DomainOrderEntity order);

    /**
     * Finds all orders with the specified statuses.
     * @param statuses Collection of order statuses to filter by
     * @return List of orders matching the statuses
     */
    List<DomainOrderEntity> findByStatusIn(Collection<SharedOrderStatusEnum> statuses);

    /**
     * Finds an order by ID with pessimistic locking.
     * @param orderId The order ID to lock and retrieve
     * @return Optional containing the locked order if found, empty otherwise
     */
    Optional<DomainOrderEntity> findByIdWithLock(UUID orderId);

    /**
     * Checks if an order exists with the given ID.
     * @param orderId The order ID to check
     * @return true if order exists, false otherwise
     */
    boolean existsById(UUID orderId);

    /**
     * Deletes an order by its ID.
     * @param orderId The order ID to delete
     */
    void deleteById(UUID orderId);
}