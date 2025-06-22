package ai.shreds.shared.exceptions;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when an error occurs while processing an incoming webhook after it has
 * successfully passed initial validation (signature, schema, etc.).
 * <p>
 * This exception deliberately captures the {@link SharedEnumPaymentProcessorType}
 * and the affected webhook identifier to facilitate structured logging and
 * downstream error‐handling (e.g. returning the proper HTTP error payload in the
 * primary adapter layer).
 */
@Getter
public class SharedExceptionWebhookProcessingException extends RuntimeException {

    private static final long serialVersionUID = 7843247908327422081L;

    private final SharedEnumPaymentProcessorType processorType;
    private final String reason;
    private final UUID webhookId;

    /**
     * Create a new processing exception without a root cause.
     *
     * @param processorType the PSP that originated the webhook
     * @param reason        human readable reason of the failure
     * @param webhookId     identifier of the webhook that failed during processing
     */
    public SharedExceptionWebhookProcessingException(final SharedEnumPaymentProcessorType processorType,
                                                     final String reason,
                                                     final UUID webhookId) {
        super(formatMessage(processorType, reason, webhookId));
        this.processorType = processorType;
        this.reason = reason;
        this.webhookId = webhookId;
    }

    /**
     * Create a new processing exception with a root cause.
     *
     * @param processorType the PSP that originated the webhook
     * @param reason        human readable reason of the failure
     * @param webhookId     identifier of the webhook that failed during processing
     * @param cause         the underlying cause of the processing failure
     */
    public SharedExceptionWebhookProcessingException(final SharedEnumPaymentProcessorType processorType,
                                                     final String reason,
                                                     final UUID webhookId,
                                                     final Throwable cause) {
        super(formatMessage(processorType, reason, webhookId), cause);
        this.processorType = processorType;
        this.reason = reason;
        this.webhookId = webhookId;
    }

    private static String formatMessage(final SharedEnumPaymentProcessorType processorType,
                                         final String reason,
                                         final UUID webhookId) {
        return String.format("Webhook processing failed for processor %s: %s (webhookId: %s)",
                             processorType, reason, webhookId);
    }
}
