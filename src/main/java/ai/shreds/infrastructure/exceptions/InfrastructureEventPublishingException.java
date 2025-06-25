package ai.shreds.infrastructure.exceptions;

public class InfrastructureEventPublishingException extends RuntimeException {
    private final String eventType;
    private final Object eventData;

    public InfrastructureEventPublishingException(String message, Throwable cause, String eventType, Object eventData) {
        super(message, cause);
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public String getEventType() {
        return eventType;
    }

    public Object getEventData() {
        return eventData;
    }
}