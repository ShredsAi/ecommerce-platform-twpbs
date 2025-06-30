package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedCartCheckoutEvent;
import ai.shreds.shared.dtos.SharedOrderConfirmedEventDTO;

public interface ApplicationReservationInputPort {
    void processCartCheckout(SharedCartCheckoutEvent event);
    void processOrderConfirmed(SharedOrderConfirmedEventDTO event);
    void processExpiredReservations(int batchSize);
}