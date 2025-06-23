package ai.shreds.domain.ports;

import ai.shreds.domain.commands.DomainThreeDSecureResult;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;

import java.util.UUID;

/**
 * Input port for 3D Secure operations in the domain.
 * This interface defines the operations related to 3D Secure authentication processing.
 */
public interface DomainInputPortThreeDSecure {

    /**
     * Initiates a 3D Secure authentication session for the given payment intent.
     * @param intent the payment intent that requires 3D Secure authentication
     * @return the created 3D Secure entity with challenge details
     */
    DomainThreeDSecureEntity initiate3DS(DomainPaymentIntentEntity intent);

    /**
     * Completes a 3D Secure authentication session with the provided result.
     * @param id the ID of the 3D Secure session
     * @param result the authentication result from the 3D Secure provider
     * @return the updated 3D Secure entity
     */
    DomainThreeDSecureEntity complete3DS(UUID id, DomainThreeDSecureResult result);
}
