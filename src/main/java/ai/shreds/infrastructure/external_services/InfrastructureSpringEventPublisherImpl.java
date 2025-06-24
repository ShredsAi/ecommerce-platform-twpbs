package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.events.DomainOrderCreatedEvent;
import ai.shreds.domain.events.DomainOrderCreationFailedEvent;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class InfrastructureSpringEventPublisherImpl implements ApplicationEventPublisherOutputPort, DomainOutputPortEventPublisher {

    private final ApplicationEventPublisher publisher;

    public InfrastructureSpringEventPublisherImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishOrderCreated(SharedOrderCreatedEventDTO event) {
        publishAfterCommit(event);
    }

    @Override
    public void publishOrderCreationFailed(SharedOrderCreationFailedEventDTO event) {
        publishAfterCommit(event);
    }

    @Override
    public void publishOrderCreated(DomainOrderCreatedEvent event) {
        publishAfterCommit(event.toSharedDTO());
    }

    @Override
    public void publishOrderCreationFailed(DomainOrderCreationFailedEvent event) {
        publishAfterCommit(event.toSharedDTO());
    }

    private void publishAfterCommit(Object event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }
}