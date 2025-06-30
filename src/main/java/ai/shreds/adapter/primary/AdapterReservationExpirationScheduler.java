package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationReservationInputPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdapterReservationExpirationScheduler {

    private final ApplicationReservationInputPort reservationService;
    
    @Value('${reservation.expiration.batch-size:100}')
    private int batchSize;

    @Scheduled(fixedDelayString = '${reservation.expiration.interval:60000}') // Default 60 seconds
    public void processExpiredReservations() {
        log.info('Starting scheduled reservation expiration processing');
        try {
            reservationService.processExpiredReservations(batchSize);
        } catch (Exception e) {
            log.error('Error during scheduled reservation expiration processing', e);
            // The scheduler continues to run even if there's an error
        }
    }
}