package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDomainEventDTO;

/**
 * Output port for publishing domain events to external channels.
 */
public interface ApplicationEventOutputPort {

    /**
     * Publish an event to Kafka.
     * @param event the domain event DTO
     */
    void publishToKafka(SharedDomainEventDTO event);

    /**
     * Publish an event to JMS.
     * @param event the domain event DTO
     */
    void publishToJms(SharedDomainEventDTO event);

    /**
     * Publish an event via Spring Application Events.
     * @param event the raw event object
     */
    void publishToSpringEvents(Object event);
}
