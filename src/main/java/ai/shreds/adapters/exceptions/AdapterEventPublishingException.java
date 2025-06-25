package ai.shreds.adapters.exceptions;

public class AdapterEventPublishingException extends RuntimeException {
    private final String eventType;
    private final Object payload;

    public AdapterEventPublishingException(String message, Throwable cause, String eventType, Object payload) {
        super(message, cause);
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }
}
