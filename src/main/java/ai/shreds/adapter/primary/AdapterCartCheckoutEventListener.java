package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationReservationInputPort;
import ai.shreds.shared.dtos.SharedCartCheckoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterCartCheckoutEventListener {

    private final ApplicationReservationInputPort reservationInputPort;

    @EventListener
    public void onCartCheckout(SharedCartCheckoutEvent event) {
        log.info("Received cart checkout event for cartId: {}", event.getCartId());
        reservationInputPort.processCartCheckout(event);
    }
}
