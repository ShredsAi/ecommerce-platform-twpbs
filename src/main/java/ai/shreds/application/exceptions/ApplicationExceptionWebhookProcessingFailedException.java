package ai.shreds.application.exceptions;

import lombok.Getter;

import java.util.UUID;

/**
 * Exception thrown when webhook processing fails with a specific reason.
 */
@Getter
public class ApplicationExceptionWebhookProcessingFailedException extends RuntimeException {
    private final UUID webhookId;
    private final String reason;

    public ApplicationExceptionWebhookProcessingFailedException(UUID webhookId, String reason, Throwable cause) {
        super(String.format("Failed to process webhook %s: %s", webhookId, reason), cause);
        this.webhookId = webhookId;
        this.reason = reason;
    }

    public ApplicationExceptionWebhookProcessingFailedException(UUID webhookId, String reason) {
        super(String.format("Failed to process webhook %s: %s", webhookId, reason));
        this.webhookId = webhookId;
        this.reason = reason;
    }
}