package ai.shreds.adapters.primary;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationEventPublisherInputPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterSpringEventListener {

    private final ApplicationEventPublisherInputPort eventPublisher;

    @EventListener
    public void handleCancellationApprovedEvent(Object event) {
        log.debug("Received cancellation approved event: {}", event);
        
        try {
            if (event == null) {
                log.warn("Received null cancellation approved event");
                return;
            }
            
            eventPublisher.publishCancellationEvent(event);
            log.debug("Successfully published cancellation approved event");
        } catch (Exception ex) {
            log.error("Error publishing cancellation approved event", ex);
            throw ex;
        }
    }

    @EventListener
    public void handleReturnProcessedEvent(Object event) {
        log.debug("Received return processed event: {}", event);
        
        try {
            if (event == null) {
                log.warn("Received null return processed event");
                return;
            }
            
            eventPublisher.publishReturnEvent(event);
            log.debug("Successfully published return processed event");
        } catch (Exception ex) {
            log.error("Error publishing return processed event", ex);
            throw ex;
        }
    }

    @EventListener
    public void handleRefundCompletedEvent(Object event) {
        log.debug("Received refund completed event: {}", event);
        
        try {
            if (event == null) {
                log.warn("Received null refund completed event");
                return;
            }
            
            // Refund completion can be related to both cancellation and return processes
            // We'll publish it as a return event since refunds are typically associated with returns
            eventPublisher.publishReturnEvent(event);
            log.debug("Successfully published refund completed event");
        } catch (Exception ex) {
            log.error("Error publishing refund completed event", ex);
            throw ex;
        }
    }
}