package ai.shreds.infrastructure.external_services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureSpringEventClient {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public InfrastructureSpringEventClient(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishEvent(Object event) {
        try {
            applicationEventPublisher.publishEvent(event);
            System.out.println("Successfully published Spring event: " + event.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("Failed to publish Spring event - eventType: " + event.getClass().getSimpleName() + 
                              ", error: " + e.getMessage());
        }
    }

    @Async
    public void publishAsyncEvent(Object event) {
        try {
            applicationEventPublisher.publishEvent(event);
            System.out.println("Successfully published async Spring event: " + event.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("Failed to publish async Spring event - eventType: " + event.getClass().getSimpleName() + 
                              ", error: " + e.getMessage());
        }
    }
}