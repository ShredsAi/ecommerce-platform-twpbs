package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationKafkaPublisherOutputPort;
import ai.shreds.shared.dtos.SharedPaymentFailedEvent;
import ai.shreds.shared.dtos.SharedPaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the Kafka publisher port for sending payment events to Kafka topics.
 * Uses Spring Kafka for reliable delivery of event messages.
 */
@Component
public class InfrastructureKafkaEventPublisher implements ApplicationKafkaPublisherOutputPort {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureKafkaEventPublisher.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;
    
    public InfrastructureKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishPaymentSucceeded(SharedPaymentSucceededEvent event) {
        log.info("Publishing payment succeeded event for payment ID: {}", event.getPaymentId());
        send(paymentEventsTopic, event.getPaymentId().toString(), event);
    }

    @Override
    public void publishPaymentFailed(SharedPaymentFailedEvent event) {
        log.info("Publishing payment failed event for payment ID: {}", event.getPaymentId());
        send(paymentEventsTopic, event.getPaymentId().toString(), event);
    }
    
    private void send(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                topic, 
                key,
                event
            );
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent event to topic {} with key {}: {}", 
                        result.getRecordMetadata().topic(),
                        key,
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send event to topic {} with key {}: {}", 
                        topic, key, ex.getMessage(), ex);
                    // In a production system, we might want to implement a retry mechanism
                    // or dead letter queue here
                }
            });
            
        } catch (Exception e) {
            log.error("Error sending event to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }
}
