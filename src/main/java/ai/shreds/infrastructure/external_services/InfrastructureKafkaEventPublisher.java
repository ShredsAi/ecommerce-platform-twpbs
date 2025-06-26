package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.infrastructure.mappers.InfrastructureEventMapper;
import ai.shreds.shared.dtos.SharedKafkaEventDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InfrastructureKafkaEventPublisher implements DomainOutputPortEventPublisher {

    private final KafkaTemplate<String, SharedKafkaEventDTO> kafkaTemplate;
    private final InfrastructureEventMapper eventMapper;
    
    private static final String ORDER_STATUS_TOPIC = "order-status-changed";
    private static final String FULFILLMENT_TOPIC = "order-fulfillment-events";

    public InfrastructureKafkaEventPublisher(KafkaTemplate<String, SharedKafkaEventDTO> kafkaTemplate,
                                             InfrastructureEventMapper eventMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventMapper = eventMapper;
    }

    @Override
    public void publishOrderStatusChanged(UUID orderId, 
                                          SharedOrderStatusEnum previousStatus, 
                                          SharedOrderStatusEnum newStatus) {
        try {
            SharedKafkaEventDTO event = eventMapper.toKafkaEvent(orderId, previousStatus, newStatus);
            kafkaTemplate.send(ORDER_STATUS_TOPIC, orderId.toString(), event);
        } catch (Exception e) {
            throw new InfrastructureServiceException(
                    "Failed to publish order status changed event for order: " + orderId,
                    "KAFKA_SERVICE",
                    "PUBLISH_FAILURE",
                    e
            );
        }
    }

    @Override
    public void publishFulfillmentEvent(UUID sagaId, UUID orderId, String step, String status) {
        try {
            SharedKafkaEventDTO event = eventMapper.toFulfillmentEvent(sagaId, orderId, step, status);
            kafkaTemplate.send(FULFILLMENT_TOPIC, orderId.toString(), event);
        } catch (Exception e) {
            throw new InfrastructureServiceException(
                    "Failed to publish fulfillment event for saga: " + sagaId + ", order: " + orderId,
                    "KAFKA_SERVICE",
                    "PUBLISH_FAILURE",
                    e
            );
        }
    }
}