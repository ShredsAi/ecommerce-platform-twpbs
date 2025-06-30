package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedReservationConfirmedEventDTO;
import ai.shreds.shared.dtos.SharedReservationCreatedEventDTO;
import ai.shreds.shared.dtos.SharedReservationExpiredEventDTO;
import ai.shreds.shared.dtos.SharedReservationFailedEventDTO;

/**
 * Output port for publishing reservation events to external systems (Kafka).
 */
public interface ApplicationEventPublisherOutputPort {
    
    /**
     * Publishes a reservation created event.
     * @param event The reservation created event
     */
    void publishReservationCreated(SharedReservationCreatedEventDTO event);
    
    /**
     * Publishes a reservation failed event.
     * @param event The reservation failed event
     */
    void publishReservationFailed(SharedReservationFailedEventDTO event);
    
    /**
     * Publishes a reservation confirmed event.
     * @param event The reservation confirmed event
     */
    void publishReservationConfirmed(SharedReservationConfirmedEventDTO event);
    
    /**
     * Publishes a reservation expired event.
     * @param event The reservation expired event
     */
    void publishReservationExpired(SharedReservationExpiredEventDTO event);
}