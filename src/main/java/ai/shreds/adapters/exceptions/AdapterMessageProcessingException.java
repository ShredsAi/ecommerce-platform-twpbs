package ai.shreds.adapters.exceptions;

/**
 * Exception thrown when message processing fails in adapter layer
 */
public class AdapterMessageProcessingException extends RuntimeException {

    private final String messageId;
    private final String messageType;
    private final String destination;

    public AdapterMessageProcessingException(String message) {
        super(message);
        this.messageId = null;
        this.messageType = null;
        this.destination = null;
    }

    public AdapterMessageProcessingException(String message, String messageId, String messageType) {
        super(message);
        this.messageId = messageId;
        this.messageType = messageType;
        this.destination = null;
    }

    public AdapterMessageProcessingException(String message, String messageId, String messageType, String destination) {
        super(message);
        this.messageId = messageId;
        this.messageType = messageType;
        this.destination = destination;
    }

    public AdapterMessageProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.messageId = null;
        this.messageType = null;
        this.destination = null;
    }

    public AdapterMessageProcessingException(String message, String messageId, String messageType, String destination, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
        this.messageType = messageType;
        this.destination = destination;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getDestination() {
        return destination;
    }
}
