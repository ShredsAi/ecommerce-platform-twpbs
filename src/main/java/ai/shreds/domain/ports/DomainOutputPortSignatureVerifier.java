package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;

/**
 * Port for verifying webhook signatures from payment processors.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortSignatureVerifier {
    /**
     * Verifies the signature of a webhook payload.
     *
     * @param rawPayload    The raw webhook payload as received from the payment processor
     * @param signature     The signature provided in the webhook headers
     * @param processorType The type of payment processor that sent the webhook
     * @return true if signature is valid, false otherwise
     */
    boolean verifySignature(String rawPayload, String signature, SharedEnumPaymentProcessorType processorType);
}
