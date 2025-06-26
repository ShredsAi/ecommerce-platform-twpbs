package ai.shreds.shared.dtos;

/**
 * DTO representing a system cancellation message.
 */
public class SharedSystemCancellationMessage {
    private String orderId;
    private String messageId;

    public SharedSystemCancellationMessage() {}

    public SharedSystemCancellationMessage(String orderId, String messageId) {
        this.orderId = orderId;
        this.messageId = messageId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}