package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationWebhookProcessedDTO;

public interface ApplicationProcessWebhookInputPort {

    /**
     * Process a PaymentWebhookProcessed event to update payment status
     * @param dto data from webhook reconciliation
     */
    void processWebhookUpdate(ApplicationWebhookProcessedDTO dto);
}