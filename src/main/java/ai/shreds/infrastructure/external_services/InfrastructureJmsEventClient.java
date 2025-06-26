package ai.shreds.infrastructure.external_services;

import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Client for publishing events to JMS.
 */
@Component
public class InfrastructureJmsEventClient {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureJmsEventClient.class);
    
    private final JmsTemplate jmsTemplate;
    private final String queueName;

    public InfrastructureJmsEventClient(JmsTemplate jmsTemplate,
                                       @Value("${spring.jms.queue.domain-events:domain-events-queue}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
    }

    public void publishEvent(SharedDomainEventDTO event) {
        try {
            jmsTemplate.convertAndSend(queueName, event);
            logger.debug("Event published successfully to JMS queue: {}", event.getEventId());
        } catch (Exception ex) {
            logger.error("Failed to publish event to JMS: {} - {}", event.getEventId(), ex.getMessage());
            throw new InfrastructureExternalServiceException("JMSService", ex.getMessage(), ex);
        }
    }

    public void publishToTopic(SharedDomainEventDTO event, String topicName) {
        try {
            jmsTemplate.convertAndSend(topicName, event);
            logger.debug("Event published successfully to JMS topic: {} - {}", topicName, event.getEventId());
        } catch (Exception ex) {
            logger.error("Failed to publish event to JMS topic: {} - {}", topicName, ex.getMessage());
            throw new InfrastructureExternalServiceException("JMSService", ex.getMessage(), ex);
        }
    }
}