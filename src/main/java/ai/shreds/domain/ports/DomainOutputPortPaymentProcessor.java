package ai.shreds.domain.ports;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;

import java.util.Map;

/**
 * Output port for payment processor operations.
 * This interface defines the contract for interacting with external payment processors.
 */
public interface DomainOutputPortPaymentProcessor {

    /**
     * Charges a payment through the external processor.
     * @param intent the payment intent to charge
     * @param threeDS the 3D Secure entity if 3DS authentication is required, null otherwise
     * @return the result of the charge operation
     */
    DomainProcessorChargeResult charge(DomainPaymentIntentEntity intent, DomainThreeDSecureEntity threeDS);

    /**
     * Parses a webhook payload from the external processor.
     * @param payload the raw webhook payload from the processor
     * @return a command containing the parsed webhook data
     */
    DomainUpdateStatusCommand parseWebhook(Map<String, Object> payload);

    /**
     * Checks if this processor can handle the given payment intent.
     * @param intent the payment intent to check
     * @return true if this processor can handle the intent, false otherwise
     */
    boolean canProcess(DomainPaymentIntentEntity intent);

    /**
     * Gets the processor type identifier.
     * @return the processor type as a string
     */
    String getProcessorType();

    /**
     * Validates webhook signature or authenticity.
     * @param payload the webhook payload
     * @param signature the signature or authentication header
     * @return true if the webhook is valid, false otherwise
     */
    boolean validateWebhook(Map<String, Object> payload, String signature);
}