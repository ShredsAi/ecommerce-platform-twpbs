package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.events.DomainOrderCreatedEvent;
import ai.shreds.domain.events.DomainOrderCreationFailedEvent;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureSpringEventPublisherImpl implements ApplicationEventPublisherOutputPort, DomainOutputPortEventPublisher {

    private final ApplicationEventPublisher publisher;

    public InfrastructureSpringEventPublisherImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishOrderCreated(SharedOrderCreatedEventDTO event) {
        // Publish immediately for integration tests and synchronous processing
        publisher.publishEvent(event);
    }

    @Override
    public void publishOrderCreationFailed(SharedOrderCreationFailedEventDTO event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishOrderCreated(DomainOrderCreatedEvent event) {
        publisher.publishEvent(event.toSharedDTO());
    }

    @Override
    public void publishOrderCreationFailed(DomainOrderCreationFailedEvent event) {
        publisher.publishEvent(event.toSharedDTO());
    }
}