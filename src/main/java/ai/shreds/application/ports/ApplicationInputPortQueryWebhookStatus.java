package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationWebhookStatusDTO;

import java.util.UUID;

/**
 * Input port for querying webhook processing status.
 */
public interface ApplicationInputPortQueryWebhookStatus {
    
    /**
     * Retrieve the current status of a processed webhook.
     *
     * @param webhookId The unique identifier of the webhook
     * @return Current status and details of the webhook
     */
    ApplicationWebhookStatusDTO getWebhookStatus(UUID webhookId);
}