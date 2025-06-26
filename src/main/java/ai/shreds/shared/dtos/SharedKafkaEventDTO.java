package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * DTO representing an event published to Kafka topics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedKafkaEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private String previousStatus;
    private String newStatus;
    private UUID sagaId;
    private String step;
    private String status;
    
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    
    /**
     * Validates the Kafka event data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
    }
    
    /**
     * Adds a key-value pair to the payload map.
     * 
     * @param key the key
     * @param value the value
     * @return this event instance for method chaining
     */
    public SharedKafkaEventDTO addPayloadItem(String key, Object value) {
        if (key != null && !key.trim().isEmpty()) {
            if (payload == null) {
                payload = new HashMap<>();
            }
            payload.put(key, value);
        }
        return this;
    }
    
    /**
     * Gets a typed value from the payload map.
     * 
     * @param key the key to retrieve
     * @param type the expected type
     * @return the value cast to the expected type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromPayload(String key, Class<T> type) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        
        Object value = payload.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Checks if this is an order status change event.
     * 
     * @return true if both previous and new status are present
     */
    public boolean isOrderStatusChangeEvent() {
        return previousStatus != null && !previousStatus.isEmpty() &&
               newStatus != null && !newStatus.isEmpty();
    }
    
    /**
     * Checks if this is a saga event.
     * 
     * @return true if sagaId, step and status are present
     */
    public boolean isSagaEvent() {
        return sagaId != null &&
               step != null && !step.isEmpty() &&
               status != null && !status.isEmpty();
    }
    
    /**
     * Factory method to create a new order status change event.
     * 
     * @param orderId the order ID
     * @param previousStatus the previous order status
     * @param newStatus the new order status
     * @return a new SharedKafkaEventDTO configured as a status change event
     */
    public static SharedKafkaEventDTO createOrderStatusChangeEvent(UUID orderId, String previousStatus, String newStatus) {
        return SharedKafkaEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType("ORDER_STATUS_CHANGED")
                .timestamp(Instant.now())
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .build();
    }
    
    /**
     * Factory method to create a new saga event.
     * 
     * @param orderId the order ID
     * @param sagaId the saga ID
     * @param step the saga step
     * @param status the saga status
     * @return a new SharedKafkaEventDTO configured as a saga event
     */
    public static SharedKafkaEventDTO createSagaEvent(UUID orderId, UUID sagaId, String step, String status) {
        return SharedKafkaEventDTO.builder()
                .eventId(UUID.randomUUID())
                .eventType("SAGA_" + step + "_" + status)
                .timestamp(Instant.now())
                .orderId(orderId)
                .sagaId(sagaId)
                .step(step)
                .status(status)
                .build();
    }
}