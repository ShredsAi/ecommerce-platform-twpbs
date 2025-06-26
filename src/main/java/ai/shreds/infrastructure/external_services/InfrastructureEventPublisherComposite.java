package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureEventPublisherComposite implements ApplicationEventOutputPort {

    private final InfrastructureKafkaEventClient kafkaEventClient;
    private final InfrastructureJmsEventClient jmsEventClient;
    private final InfrastructureSpringEventClient springEventClient;

    @Autowired
    public InfrastructureEventPublisherComposite(InfrastructureKafkaEventClient kafkaEventClient,
                                                InfrastructureJmsEventClient jmsEventClient,
                                                InfrastructureSpringEventClient springEventClient) {
        this.kafkaEventClient = kafkaEventClient;
        this.jmsEventClient = jmsEventClient;
        this.springEventClient = springEventClient;
    }

    @Override
    public void publishToKafka(SharedDomainEventDTO event) {
        try {
            logEventPublication(event, "Kafka");
            kafkaEventClient.publishEvent(event);
        } catch (Exception e) {
            System.err.println("Failed to publish event to Kafka: " + event.getEventId() + ", error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void publishToJms(SharedDomainEventDTO event) {
        try {
            logEventPublication(event, "JMS");
            jmsEventClient.publishEvent(event);
        } catch (Exception e) {
            System.err.println("Failed to publish event to JMS: " + event.getEventId() + ", error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void publishToSpringEvents(Object event) {
        try {
            logEventPublication(event, "SpringEvents");
            springEventClient.publishEvent(event);
        } catch (Exception e) {
            System.err.println("Failed to publish Spring event: " + event.getClass().getSimpleName() + ", error: " + e.getMessage());
            throw e;
        }
    }

    private void logEventPublication(Object event, String destination) {
        if (event instanceof SharedDomainEventDTO) {
            SharedDomainEventDTO domainEvent = (SharedDomainEventDTO) event;
            System.out.println("Publishing domain event - ID: " + domainEvent.getEventId() + 
                              ", Type: " + domainEvent.getEventType() + 
                              ", Destination: " + destination);
        } else {
            System.out.println("Publishing event - Type: " + event.getClass().getSimpleName() + 
                              ", Destination: " + destination);
        }
    }
}