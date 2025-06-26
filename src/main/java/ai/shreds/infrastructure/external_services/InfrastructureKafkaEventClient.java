package ai.shreds.infrastructure.external_services;

import ai.shreds.shared.dtos.SharedDomainEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

@Component
public class InfrastructureKafkaEventClient {

    private final KafkaTemplate<String, SharedDomainEventDTO> kafkaTemplate;
    private final String topicName;

    @Autowired
    public InfrastructureKafkaEventClient(KafkaTemplate<String, SharedDomainEventDTO> kafkaTemplate,
                                        @Value("${kafka.topic.domain-events:domain-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publishEvent(SharedDomainEventDTO event) {
        try {
            ListenableFuture<SendResult<String, SharedDomainEventDTO>> future = 
                kafkaTemplate.send(topicName, event.getAggregateId(), event);
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, SharedDomainEventDTO>>() {
                @Override
                public void onSuccess(SendResult<String, SharedDomainEventDTO> result) {
                    System.out.println("Successfully published event: " + event.getEventId() + 
                                     " to topic: " + topicName);
                }

                @Override
                public void onFailure(Throwable ex) {
                    handleKafkaError(event, (Exception) ex);
                }
            });
        } catch (Exception e) {
            handleKafkaError(event, e);
        }
    }

    public void publishBatch(List<SharedDomainEventDTO> events) {
        for (SharedDomainEventDTO event : events) {
            publishEvent(event);
        }
    }

    private void handleKafkaError(SharedDomainEventDTO event, Exception ex) {
        System.err.println("Failed to publish event to Kafka - eventId: " + event.getEventId() + 
                          ", error: " + ex.getMessage());
        // Could implement retry logic or dead letter queue here
    }
}