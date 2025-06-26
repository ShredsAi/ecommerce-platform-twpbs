package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedOrderEventMessage;

/**
 * Input port for handling incoming order JMS events.
 */
public interface ApplicationOrderEventInputPort {
    /**
     * Handle an incoming order event message.
     * @param message the shared order event message
     */
    void handleOrderEvent(SharedOrderEventMessage message);
}