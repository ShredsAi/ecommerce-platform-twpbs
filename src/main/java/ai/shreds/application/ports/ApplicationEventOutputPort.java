package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDomainEventDTO;

public interface ApplicationEventOutputPort {

    void publishToKafka(SharedDomainEventDTO event);

    void publishToJms(SharedDomainEventDTO event);

    void publishToSpringEvents(Object event);
}