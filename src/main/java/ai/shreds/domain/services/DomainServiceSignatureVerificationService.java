package ai.shreds.domain.services;

import ai.shreds.domain.exceptions.DomainExceptionInvalidSignatureException;
import ai.shreds.domain.ports.DomainOutputPortSignatureVerifier;
import ai.shreds.domain.value_objects.DomainWebhookCommand;

/**
 * Domain service for verifying webhook signatures.
 */
public class DomainServiceSignatureVerificationService {
    private final DomainOutputPortSignatureVerifier signatureVerifierPort;

    public DomainServiceSignatureVerificationService(DomainOutputPortSignatureVerifier signatureVerifierPort) {
        this.signatureVerifierPort = signatureVerifierPort;
    }

    /**
     * Verifies the signature of a webhook command.
     *
     * @param command The webhook command containing payload and signature
     * @return true if signature is valid
     * @throws DomainExceptionInvalidSignatureException if signature verification fails
     */
    public boolean verifyWebhookSignature(DomainWebhookCommand command) {
        boolean isValid = signatureVerifierPort.verifySignature(
            command.getRawPayload(),
            command.getSignature(),
            command.getProcessorType()
        );

        if (!isValid) {
            throw new DomainExceptionInvalidSignatureException(
                command.getProcessorType(),
                command.getExternalEventId()
            );
        }

        return true;
    }
}
