package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationReservationInputPort;
import ai.shreds.shared.dtos.SharedOrderConfirmedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdapterOrderConfirmedEventListener {
    
    private final ApplicationReservationInputPort reservationService;
    
    @EventListener
    public void handleOrderConfirmed(SharedOrderConfirmedEventDTO event) {
        log.info("Received order confirmed event for orderId: {}", event.getOrderId());
        reservationService.processOrderConfirmed(event);
    }
}
