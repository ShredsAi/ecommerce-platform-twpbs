package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

/**
 * Port for checking webhook idempotency to prevent duplicate processing.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortIdempotencyService {
    /**
     * Checks if a webhook with the given external event ID and processor type has already been processed.
     *
     * @param externalEventId The external event ID from the payment processor
     * @param processorType   The payment processor type
     * @return true if the webhook has already been processed, false otherwise
     */
    boolean isDuplicate(String externalEventId, SharedEnumPaymentProcessorType processorType);
}
