package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPaymentEvent;

/**
 * Port for publishing payment events to message broker.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortEventPublisher {
    /**
     * Publishes a payment event to the message broker.
     *
     * @param event The payment event to publish
     */
    void publishPaymentEvent(DomainEntityPaymentEvent event);
}
