package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import ai.shreds.adapters.exceptions.AdapterEventPublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AdapterSpringEventPublisher implements ApplicationEventPublisherOutputPort {

    private static final Logger logger = LoggerFactory.getLogger(AdapterSpringEventPublisher.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdapterSpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishOrderCreated(SharedOrderCreatedEventDTO event) {
        try {
            addEventHeaders(event, event.getCorrelationId());
            applicationEventPublisher.publishEvent(event);
            logger.info("Published OrderCreatedEvent for orderId={}, correlationId={}...", event.getOrderId(), event.getCorrelationId());
        } catch (Exception e) {
            throw new AdapterEventPublishingException(
                "Error publishing OrderCreatedEvent",
                e,
                "OrderCreatedEvent",
                event
            );
        }
    }

    @Override
    public void publishOrderCreationFailed(SharedOrderCreationFailedEventDTO event) {
        try {
            addEventHeaders(event, event.getCorrelationId());
            applicationEventPublisher.publishEvent(event);
            logger.info("Published OrderCreationFailedEvent for cartId={}, correlationId={}...", event.getCartId(), event.getCorrelationId());
        } catch (Exception e) {
            throw new AdapterEventPublishingException(
                "Error publishing OrderCreationFailedEvent",
                e,
                "OrderCreationFailedEvent",
                event
            );
        }
    }

    private void addEventHeaders(Object event, String correlationId) {
        logger.debug("Adding event header correlationId={} to event {}", correlationId, event.getClass().getSimpleName());
        // Additional header logic if needed
    }
}
