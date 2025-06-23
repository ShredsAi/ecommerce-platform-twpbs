package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.infrastructure.dtos.InfrastructureKafkaEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureEventPublisherImpl implements DomainOutputPortEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "payment-events";

    public InfrastructureEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPaymentEvent(DomainEntityPaymentEvent event) {
        try {
            InfrastructureKafkaEventDTO kafkaEvent = toKafkaDTO(event);
            String payload = objectMapper.writeValueAsString(kafkaEvent);
            kafkaTemplate.send(TOPIC, event.getPaymentId().toString(), payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }

    private InfrastructureKafkaEventDTO toKafkaDTO(DomainEntityPaymentEvent event) {
        return InfrastructureKafkaEventDTO.fromDomainEntity(event);
    }
}
