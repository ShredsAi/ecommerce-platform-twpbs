package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationInputPortHandleShippingUpdate;
import ai.shreds.shared.dtos.SharedShippingUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter for handling shipping update stream observations.
 * Receives shipping updates and forwards them to the application layer.
 */
@Component
public class AdapterShippingUpdateStreamObserver {

    private static final Logger logger = LoggerFactory.getLogger(AdapterShippingUpdateStreamObserver.class);

    private final ApplicationInputPortHandleShippingUpdate applicationInputPort;

    public AdapterShippingUpdateStreamObserver(ApplicationInputPortHandleShippingUpdate applicationInputPort) {
        this.applicationInputPort = applicationInputPort;
    }

    /**
     * Handles the next shipping update from the stream.
     *
     * @param update the shipping update to process
     */
    public void onNext(SharedShippingUpdateDTO update) {
        try {
            logger.debug("Received shipping update for order: {}", update.getOrderId());
            applicationInputPort.handleShippingUpdate(update.toApplicationDTO());
            logger.info("Successfully processed shipping update for order: {}", update.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to process shipping update for order: {}", update.getOrderId(), e);
            throw new RuntimeException("Failed to process shipping update", e);
        }
    }

    /**
     * Handles errors in the shipping update stream.
     *
     * @param throwable the error that occurred
     */
    public void onError(Throwable throwable) {
        logger.error("Error in shipping update stream", throwable);
        // Implementation for error handling - could trigger reconnection logic
    }

    /**
     * Handles completion of the shipping update stream.
     */
    public void onCompleted() {
        logger.info("Shipping update stream completed");
        // Implementation for stream completion - could trigger cleanup or reconnection
    }
}