package ai.shreds.domain.ports;

import ai.shreds.domain.events.DomainOrderCreatedEvent;
import ai.shreds.domain.events.DomainOrderCreationFailedEvent;

/**
 * Domain output port for publishing domain events.
 * This port is implemented by infrastructure layer.
 */
public interface DomainOutputPortEventPublisher {
    
    /**
     * Publishes an order created event.
     *
     * @param event the order created event to publish
     */
    void publishOrderCreated(DomainOrderCreatedEvent event);
    
    /**
     * Publishes an order creation failed event.
     *
     * @param event the order creation failed event to publish
     */
    void publishOrderCreationFailed(DomainOrderCreationFailedEvent event);
}