package ai.shreds.domain.ports;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;

/**
 * Input port for payment service operations in the domain.
 * This interface defines the operations related to payment processing and status management.
 */
public interface DomainInputPortPaymentService {

    /**
     * Records a payment based on the payment intent and processor charge result.
     * @param intent the payment intent that was processed
     * @param result the result from the payment processor
     * @return the created payment entity
     */
    DomainPaymentEntity recordPayment(DomainPaymentIntentEntity intent, DomainProcessorChargeResult result);

    /**
     * Handles webhook status updates for payments.
     * This method processes incoming webhook notifications to update payment statuses.
     * @param command the command containing the webhook status update details
     */
    void handleWebhookStatus(DomainUpdateStatusCommand command);
}
