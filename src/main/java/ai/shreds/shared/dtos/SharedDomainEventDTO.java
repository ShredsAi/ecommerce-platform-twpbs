package ai.shreds.shared.dtos;

public class SharedDomainEventDTO {
    private String eventId;
    private String aggregateId;
    private String eventType;

    public SharedDomainEventDTO() {
    }

    public SharedDomainEventDTO(String eventId, String aggregateId, String eventType) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}