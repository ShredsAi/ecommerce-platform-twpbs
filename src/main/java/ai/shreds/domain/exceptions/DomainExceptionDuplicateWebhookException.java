package ai.shreds.domain.exceptions;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

/**
 * Exception thrown when a duplicate webhook is detected for idempotency.
 */
public class DomainExceptionDuplicateWebhookException extends RuntimeException {
    private final String externalEventId;
    private final SharedEnumPaymentProcessorType processorType;

    public DomainExceptionDuplicateWebhookException(
            String externalEventId,
            SharedEnumPaymentProcessorType processorType) {
        super(String.format("Duplicate webhook with externalEventId=%s and processorType=%s", externalEventId, processorType));
        this.externalEventId = externalEventId;
        this.processorType = processorType;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public SharedEnumPaymentProcessorType getProcessorType() {
        return processorType;
    }
}
