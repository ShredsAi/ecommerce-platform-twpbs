package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEventEntity;

import java.util.List;

/**
 * Domain output port for order event repository operations.
 * Defines the contract for order event persistence operations.
 * This interface is implemented by the infrastructure layer.
 */
public interface DomainOutputPortOrderEventRepository {
    
    /**
     * Save an order event entity.
     * 
     * @param event the order event entity to save
     * @return the saved order event entity
     */
    DomainOrderEventEntity save(DomainOrderEventEntity event);
    
    /**
     * Find all order events for a specific order.
     * 
     * @param orderId the ID of the order
     * @return list of order event entities for the order, typically ordered by timestamp
     */
    List<DomainOrderEventEntity> findByOrderId(String orderId);
    
    /**
     * Find all order events of a specific type.
     * 
     * @param eventType the type of event to filter by
     * @return list of order event entities with the specified type
     */
    List<DomainOrderEventEntity> findByEventType(String eventType);
}