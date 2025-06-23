package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import java.util.UUID;

/**
 * Port for managing webhook-payment correlations.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortCorrelationService {
    /**
     * Saves a webhook correlation record.
     *
     * @param correlation The correlation record to save
     */
    void saveCorrelation(DomainEntityPaymentWebhookCorrelation correlation);

    /**
     * Finds a correlation record by webhook ID.
     *
     * @param webhookId The webhook ID to search for
     * @return The correlation record if found, null otherwise
     */
    DomainEntityPaymentWebhookCorrelation findCorrelationByWebhookId(UUID webhookId);
}
