package ai.shreds.application.ports;

/**
 * Input port for publishing application-level events via Spring's event system.
 */
public interface ApplicationEventPublisherInputPort {
    /**
     * Publish a cancellation-related event to downstream listeners.
     * @param event the cancellation event object
     */
    void publishCancellationEvent(Object event);

    /**
     * Publish a return-related event to downstream listeners.
     * @param event the return processed event object
     */
    void publishReturnEvent(Object event);
}