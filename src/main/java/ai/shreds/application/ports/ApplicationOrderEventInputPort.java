package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedOrderEventMessage;

/**
 * Port for handling order event messages in the application layer.
 */
public interface ApplicationOrderEventInputPort {

    /**
     * Handle an incoming order event message.
     * @param message the shared order event message
     */
    void handleOrderEvent(SharedOrderEventMessage message);
}
