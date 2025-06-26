package ai.shreds.infrastructure.external_services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Client for publishing events to Spring Application Event system.
 */
@Component
public class InfrastructureSpringEventClient {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureSpringEventClient.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;

    public InfrastructureSpringEventClient(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishEvent(Object event) {
        try {
            applicationEventPublisher.publishEvent(event);
            logger.debug("Event published successfully to Spring Application Events: {}", event.getClass().getSimpleName());
        } catch (Exception ex) {
            logger.error("Failed to publish event to Spring Application Events: {}", ex.getMessage());
            throw new RuntimeException("Failed to publish Spring event", ex);
        }
    }

    @Async
    public void publishAsyncEvent(Object event) {
        try {
            applicationEventPublisher.publishEvent(event);
            logger.debug("Async event published successfully to Spring Application Events: {}", event.getClass().getSimpleName());
        } catch (Exception ex) {
            logger.error("Failed to publish async event to Spring Application Events: {}", ex.getMessage());
            throw new RuntimeException("Failed to publish async Spring event", ex);
        }
    }
}