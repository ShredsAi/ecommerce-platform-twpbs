package ai.shreds.application.exceptions;

import lombok.Getter;

import java.util.UUID;

/**
 * Exception thrown when a requested webhook cannot be found.
 */
@Getter
public class ApplicationExceptionWebhookNotFoundException extends RuntimeException {
    private final UUID webhookId;

    public ApplicationExceptionWebhookNotFoundException(UUID webhookId) {
        super(String.format("Webhook with ID %s not found", webhookId));
        this.webhookId = webhookId;
    }
}