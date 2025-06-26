package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.shared.Pageable;
import java.util.List;
import java.util.UUID;

/**
 * Domain output port for order event repository operations.
 * This port defines the contract for order event persistence operations.
 */
public interface DomainOutputPortOrderEventRepository {

    /**
     * Saves an order event entity.
     * @param event The order event entity to save
     * @return The saved order event entity
     * @throws ai.shreds.domain.exceptions.DomainValidationException if event data is invalid
     */
    DomainOrderEventEntity save(DomainOrderEventEntity event);

    /**
     * Finds all order events for a specific order with pagination.
     * @param orderId The order ID to search events for
     * @param pageable Pagination information
     * @return List of order events for the specified order
     */
    List<DomainOrderEventEntity> findAllByOrderId(UUID orderId, Pageable pageable);

    /**
     * Counts the total number of events for a specific order.
     * @param orderId The order ID to count events for
     * @return The total count of events for the order
     */
    Long countByOrderId(UUID orderId);

    /**
     * Finds the latest event for a specific order.
     * @param orderId The order ID to find latest event for
     * @return The most recent order event
     */
    DomainOrderEventEntity findLatestByOrderId(UUID orderId);

    /**
     * Finds all events of a specific type for an order.
     * @param orderId The order ID to search events for
     * @param eventType The event type to filter by
     * @return List of order events matching the criteria
     */
    List<DomainOrderEventEntity> findByOrderIdAndEventType(UUID orderId, String eventType);
}