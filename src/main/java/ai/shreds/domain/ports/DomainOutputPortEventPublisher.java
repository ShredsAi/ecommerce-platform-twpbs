package ai.shreds.domain.ports;

/**
 * Output port for event publishing operations.
 * This interface defines the contract for publishing domain events to external systems.
 */
public interface DomainOutputPortEventPublisher {

    /**
     * Publishes a domain event to the appropriate event channel.
     * @param event the domain event to publish
     */
    void publish(Object event);

    /**
     * Publishes an event asynchronously.
     * @param event the domain event to publish asynchronously
     */
    void publishAsync(Object event);

    /**
     * Publishes an event to a specific topic or channel.
     * @param event the domain event to publish
     * @param topic the specific topic or channel to publish to
     */
    void publishToTopic(Object event, String topic);

    /**
     * Checks if the event publisher is available and ready to publish events.
     * @return true if the publisher is available, false otherwise
     */
    boolean isAvailable();
}