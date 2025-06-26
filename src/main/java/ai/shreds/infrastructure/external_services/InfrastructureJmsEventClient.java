package ai.shreds.infrastructure.external_services;

import ai.shreds.shared.dtos.SharedDomainEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InfrastructureJmsEventClient {

    private final JmsTemplate jmsTemplate;
    private final String queueName;
    private final ObjectMapper objectMapper;

    @Autowired
    public InfrastructureJmsEventClient(JmsTemplate jmsTemplate,
                                        @Value("${jms.queue.domain-events:domain-events-queue}") String queueName,
                                        ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(SharedDomainEventDTO event) {
        try {
            jmsTemplate.send(queueName, new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {
                    return convertToJmsMessage(event, session);
                }
            });
            System.out.println("Successfully published event to JMS queue: " + event.getEventId());
        } catch (Exception e) {
            System.err.println("Failed to publish event to JMS - eventId: " + event.getEventId() +
                              ", error: " + e.getMessage());
        }
    }

    public void publishToTopic(SharedDomainEventDTO event, String topicName) {
        try {
            jmsTemplate.send(topicName, new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {
                    return convertToJmsMessage(event, session);
                }
            });
            System.out.println("Successfully published event to JMS topic: " + topicName +
                              ", eventId: " + event.getEventId());
        } catch (Exception e) {
            System.err.println("Failed to publish event to JMS topic - eventId: " + event.getEventId() +
                              ", error: " + e.getMessage());
        }
    }

    private Message convertToJmsMessage(SharedDomainEventDTO event, Session session) throws JMSException {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            TextMessage message = session.createTextMessage(jsonMessage);
            message.setStringProperty("eventId", event.getEventId());
            message.setStringProperty("eventType", event.getEventType());
            message.setStringProperty("aggregateId", event.getAggregateId());
            message.setStringProperty("source", event.getSource());
            message.setLongProperty("timestamp", event.getTimestamp().getEpochSecond());
            return message;
        } catch (Exception e) {
            JMSException jmse = new JMSException("Failed to convert domain event to JMS message: " + e.getMessage());
            jmse.initCause(e);
            throw jmse;
        }
    }
}
