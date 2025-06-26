package ai.shreds.infrastructure.mappers;

import ai.shreds.shared.dtos.SharedKafkaEventDTO;
import ai.shreds.shared.dtos.SharedNotificationRequestDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class InfrastructureEventMapper {

    public SharedKafkaEventDTO toKafkaEvent(UUID orderId, 
                                            SharedOrderStatusEnum previousStatus, 
                                            SharedOrderStatusEnum newStatus) {
        return SharedKafkaEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType("ORDER_STATUS_CHANGED")
                .timestamp(Instant.now())
                .orderId(orderId)
                .previousStatus(previousStatus.name())
                .newStatus(newStatus.name())
                .sagaId(null)
                .step(null)
                .status(null)
                .payload(createStatusChangePayload(orderId, previousStatus, newStatus))
                .build();
    }

    public SharedKafkaEventDTO toFulfillmentEvent(UUID sagaId, UUID orderId, String step, String status) {
        return SharedKafkaEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType("SAGA_STEP_COMPLETED")
                .timestamp(Instant.now())
                .orderId(orderId)
                .previousStatus(null)
                .newStatus(null)
                .sagaId(sagaId)
                .step(step)
                .status(status)
                .payload(createFulfillmentPayload(sagaId, orderId, step, status))
                .build();
    }

    public SharedNotificationRequestDTO toNotificationRequest(String customerId, 
                                                             String type, 
                                                             Map<String, Object> data) {
        return SharedNotificationRequestDTO.builder()
                .customerId(customerId)
                .type(type)
                .data(data)
                .priority(determinePriority(type))
                .timestamp(Instant.now())
                .build();
    }

    private Map<String, Object> createStatusChangePayload(UUID orderId, 
                                                         SharedOrderStatusEnum previousStatus, 
                                                         SharedOrderStatusEnum newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("previousStatus", previousStatus.name());
        payload.put("newStatus", newStatus.name());
        payload.put("changedAt", Instant.now().toString());
        return payload;
    }

    private Map<String, Object> createFulfillmentPayload(UUID sagaId, 
                                                        UUID orderId, 
                                                        String step, 
                                                        String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sagaId", sagaId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("step", step);
        payload.put("status", status);
        payload.put("processedAt", Instant.now().toString());
        return payload;
    }

    private String determinePriority(String notificationType) {
        switch (notificationType) {
            case "ORDER_CANCELLED":
            case "PAYMENT_FAILED":
                return "HIGH";
            case "ORDER_SHIPPED":
            case "ORDER_DELIVERED":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }
}