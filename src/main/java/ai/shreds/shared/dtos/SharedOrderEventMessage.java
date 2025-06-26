package ai.shreds.shared.dtos;

import java.util.UUID;

/**
 * DTO representing an order event message from JMS.
 */
public class SharedOrderEventMessage {
    private UUID orderId;
    private UUID eventId;
    private String eventType;

    public SharedOrderEventMessage() {
    }

    public SharedOrderEventMessage(UUID orderId, UUID eventId, String eventType) {
        this.orderId = orderId;
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}