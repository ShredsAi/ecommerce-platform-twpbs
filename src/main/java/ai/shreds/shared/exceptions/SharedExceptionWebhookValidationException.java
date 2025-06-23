package ai.shreds.shared.exceptions;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import lombok.Getter;

import java.util.UUID;

/**
 * Indicates that an incoming webhook failed validation – e.g. signature mismatch,
 * unsupported event type, malformed payload, etc.
 */
@Getter
public class SharedExceptionWebhookValidationException extends RuntimeException {

    private static final long serialVersionUID = -5635489012800976008L;

    private final SharedEnumPaymentProcessorType processorType;
    private final String reason;
    private final UUID webhookId;

    public SharedExceptionWebhookValidationException(final SharedEnumPaymentProcessorType processorType,
                                                     final String reason,
                                                     final UUID webhookId) {
        super(String.format("Webhook validation failed for processor %s: %s (webhookId: %s)",
                processorType, reason, webhookId));
        this.processorType = processorType;
        this.reason = reason;
        this.webhookId = webhookId;
    }
}
