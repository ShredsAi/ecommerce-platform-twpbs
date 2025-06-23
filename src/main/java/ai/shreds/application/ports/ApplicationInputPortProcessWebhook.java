package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationWebhookDTO;
import ai.shreds.application.dtos.ApplicationWebhookResultDTO;

/**
 * Input port for processing incoming webhooks.
 */
public interface ApplicationInputPortProcessWebhook {
    
    /**
     * Process an incoming webhook from any supported payment processor.
     *
     * @param webhookDTO The webhook data to process
     * @return Result of webhook processing including correlation status
     */
    ApplicationWebhookResultDTO processWebhook(ApplicationWebhookDTO webhookDTO);
}