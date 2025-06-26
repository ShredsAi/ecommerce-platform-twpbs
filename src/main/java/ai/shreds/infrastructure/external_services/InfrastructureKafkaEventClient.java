package ai.shreds.infrastructure.external_services;

import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client for publishing events to Kafka.
 */
@Component
public class InfrastructureKafkaEventClient {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureKafkaEventClient.class);
    
    private final KafkaTemplate<String, SharedDomainEventDTO> kafkaTemplate;
    private final String topicName;

    public InfrastructureKafkaEventClient(KafkaTemplate<String, SharedDomainEventDTO> kafkaTemplate,
                                         @Value("${spring.kafka.topic.domain-events:domain-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishEvent(SharedDomainEventDTO event) {
        try {
            CompletableFuture<SendResult<String, SharedDomainEventDTO>> future = 
                kafkaTemplate.send(topicName, event.getAggregateId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    handleKafkaError(event, ex);
                } else {
                    logger.debug("Event published successfully to Kafka: {}", event.getEventId());
                }
            });
        } catch (Exception ex) {
            handleKafkaError(event, ex);
        }
    }

    public void publishBatch(List<SharedDomainEventDTO> events) {
        for (SharedDomainEventDTO event : events) {
            publishEvent(event);
        }
    }

    private void handleKafkaError(SharedDomainEventDTO event, Throwable ex) {
        logger.error("Failed to publish event to Kafka: {} - {}", event.getEventId(), ex.getMessage());
        throw new InfrastructureExternalServiceException("KafkaService", ex.getMessage(), ex);
    }
}