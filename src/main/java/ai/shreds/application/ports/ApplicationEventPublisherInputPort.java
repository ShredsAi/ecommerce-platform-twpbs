package ai.shreds.application.ports;

/**
 * Input port for publishing domain events within the application.
 */
public interface ApplicationEventPublisherInputPort {

    /**
     * Publish a cancellation event.
     * @param event the event payload
     */
    void publishCancellationEvent(Object event);

    /**
     * Publish a return event.
     * @param event the event payload
     */
    void publishReturnEvent(Object event);
}
