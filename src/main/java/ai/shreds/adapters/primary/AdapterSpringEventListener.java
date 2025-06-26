package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationEventPublisherInputPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdapterSpringEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AdapterSpringEventListener.class);
    
    private final ApplicationEventPublisherInputPort eventPublisher;

    @Autowired
    public AdapterSpringEventListener(ApplicationEventPublisherInputPort eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void handleCancellationApprovedEvent(Object event) {
        try {
            logger.info("Received cancellation approved event: {}", event.getClass().getSimpleName());
            
            eventPublisher.publishCancellationEvent(event);
            
            logger.info("Successfully published cancellation approved event");
        } catch (Exception ex) {
            logger.error("Failed to handle cancellation approved event: {}", event.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    @EventListener
    public void handleReturnProcessedEvent(Object event) {
        try {
            logger.info("Received return processed event: {}", event.getClass().getSimpleName());
            
            eventPublisher.publishReturnEvent(event);
            
            logger.info("Successfully published return processed event");
        } catch (Exception ex) {
            logger.error("Failed to handle return processed event: {}", event.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    @EventListener
    public void handleRefundCompletedEvent(Object event) {
        try {
            logger.info("Received refund completed event: {}", event.getClass().getSimpleName());
            
            eventPublisher.publishCancellationEvent(event);
            
            logger.info("Successfully published refund completed event");
        } catch (Exception ex) {
            logger.error("Failed to handle refund completed event: {}", event.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}