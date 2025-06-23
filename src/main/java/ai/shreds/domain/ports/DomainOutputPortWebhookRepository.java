package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

import java.util.List;
import java.util.UUID;

/**
 * Output port for webhook repository operations
 * Implemented by infrastructure layer
 */
public interface DomainOutputPortWebhookRepository {
    /**
     * Saves a webhook entity
     * @param webhook the webhook to save
     * @return the saved webhook entity with any generated IDs
     */
    DomainEntityPaymentWebhook save(DomainEntityPaymentWebhook webhook);
    
    /**
     * Finds a webhook by its ID
     * @param id the webhook UUID
     * @return the webhook entity if found
     */
    DomainEntityPaymentWebhook findById(UUID id);
    
    /**
     * Finds a webhook by its external event ID and processor type
     * Used for idempotency checks
     * @param externalEventId the external processor's event ID
     * @param processorType the payment processor type
     * @return the webhook entity if found
     */
    DomainEntityPaymentWebhook findByExternalEventIdAndProcessorType(String externalEventId, SharedEnumPaymentProcessorType processorType);
    
    /**
     * Finds all webhooks with pending processing status
     * @return list of pending webhooks
     */
    List<DomainEntityPaymentWebhook> findPendingWebhooks();
}