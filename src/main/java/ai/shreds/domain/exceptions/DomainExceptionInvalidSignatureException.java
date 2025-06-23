package ai.shreds.domain.exceptions;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

/**
 * Exception thrown when a webhook signature verification fails.
 */
public class DomainExceptionInvalidSignatureException extends RuntimeException {
    private final SharedEnumPaymentProcessorType processorType;
    private final String webhookId;

    public DomainExceptionInvalidSignatureException(
            SharedEnumPaymentProcessorType processorType,
            String webhookId) {
        super(String.format("Invalid signature for %s webhook with ID %s", processorType, webhookId));
        this.processorType = processorType;
        this.webhookId = webhookId;
    }

    public SharedEnumPaymentProcessorType getProcessorType() {
        return processorType;
    }

    public String getWebhookId() {
        return webhookId;
    }
}
