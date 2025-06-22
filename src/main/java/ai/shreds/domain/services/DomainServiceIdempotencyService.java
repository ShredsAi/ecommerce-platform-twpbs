package ai.shreds.domain.services;

import ai.shreds.domain.exceptions.DomainExceptionDuplicateWebhookException;
import ai.shreds.domain.ports.DomainOutputPortIdempotencyService;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

/**
 * Domain service for ensuring idempotent webhook processing.
 */
public class DomainServiceIdempotencyService {
    private final DomainOutputPortIdempotencyService idempotencyPort;

    public DomainServiceIdempotencyService(DomainOutputPortIdempotencyService idempotencyPort) {
        this.idempotencyPort = idempotencyPort;
    }

    /**
     * Checks if a webhook has already been processed and throws exception if duplicate.
     *
     * @param externalEventId The external event ID from the payment processor
     * @param processorType The payment processor type
     * @return true if webhook can be processed
     * @throws DomainExceptionDuplicateWebhookException if webhook is duplicate
     */
    public boolean checkAndMarkProcessed(String externalEventId, SharedEnumPaymentProcessorType processorType) {
        if (idempotencyPort.isDuplicate(externalEventId, processorType)) {
            throw new DomainExceptionDuplicateWebhookException(externalEventId, processorType);
        }
        
        return true;
    }
}
