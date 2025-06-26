package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationPaymentWebhookDTO;
import ai.shreds.application.dtos.ApplicationPaymentWebhookResultDTO;

/**
 * Input port for handling payment webhook events.
 */
public interface ApplicationInputPortHandlePaymentWebhook {

    /**
     * Processes a payment webhook callback.
     *
     * @param payment DTO containing payment webhook details
     * @return result of processing the payment webhook
     */
    ApplicationPaymentWebhookResultDTO handlePaymentWebhook(ApplicationPaymentWebhookDTO payment);
}
