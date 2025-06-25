package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Composite event publisher that delegates to specific event clients.
 */
@Service
public class InfrastructureEventPublisherComposite implements ApplicationEventOutputPort {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureEventPublisherComposite.class);
    
    private final InfrastructureKafkaEventClient kafkaEventClient;
    private final InfrastructureJmsEventClient jmsEventClient;
    private final InfrastructureSpringEventClient springEventClient;

    public InfrastructureEventPublisherComposite(
            InfrastructureKafkaEventClient kafkaEventClient,
            InfrastructureJmsEventClient jmsEventClient,
            InfrastructureSpringEventClient springEventClient) {
        this.kafkaEventClient = kafkaEventClient;
        this.jmsEventClient = jmsEventClient;
        this.springEventClient = springEventClient;
    }

    @Override
    public void publishToKafka(SharedDomainEventDTO event) {
        logEventPublication(event, "Kafka");
        kafkaEventClient.publishEvent(event);
    }

    @Override
    public void publishToJms(SharedDomainEventDTO event) {
        logEventPublication(event, "JMS");
        jmsEventClient.publishEvent(event);
    }

    @Override
    public void publishToSpringEvents(Object event) {
        logEventPublication(event, "Spring Events");
        springEventClient.publishEvent(event);
    }

    private void logEventPublication(Object event, String destination) {
        logger.info("Publishing event to {}: {}", destination, 
            event instanceof SharedDomainEventDTO ? 
            ((SharedDomainEventDTO) event).getEventId() : 
            event.getClass().getSimpleName());
    }
}