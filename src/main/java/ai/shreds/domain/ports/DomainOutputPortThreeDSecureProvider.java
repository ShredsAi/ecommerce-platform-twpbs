package ai.shreds.domain.ports;

import ai.shreds.domain.commands.DomainThreeDSecureResult;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;

/**
 * Output port for 3D Secure provider operations.
 * This interface defines the contract for interacting with external 3D Secure authentication services.
 */
public interface DomainOutputPortThreeDSecureProvider {

    /**
     * Initiates a 3D Secure authentication process for a payment intent.
     * @param intent the payment intent requiring 3D Secure authentication
     * @return the result containing challenge URL and authentication details
     */
    DomainThreeDSecureResult initiateAuthentication(DomainPaymentIntentEntity intent);

    /**
     * Verifies the result of a 3D Secure authentication challenge.
     * @param authenticationId the ID of the authentication session
     * @return the verification result
     */
    DomainThreeDSecureResult verifyAuthentication(String authenticationId);

    /**
     * Checks if 3D Secure authentication is required for the given payment intent.
     * @param intent the payment intent to check
     * @return true if 3D Secure is required, false otherwise
     */
    boolean isAuthenticationRequired(DomainPaymentIntentEntity intent);

    /**
     * Gets the supported 3D Secure version for the provider.
     * @return the 3D Secure version (e.g., "2.1.0")
     */
    String getThreeDSecureVersion();

    /**
     * Validates the authenticity of a 3D Secure callback.
     * @param callbackData the callback data from the 3DS provider
     * @param signature the signature to verify authenticity
     * @return true if the callback is authentic, false otherwise
     */
    boolean validateCallback(Object callbackData, String signature);
}