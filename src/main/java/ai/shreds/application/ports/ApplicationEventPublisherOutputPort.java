package ai.shreds.application.ports;

import java.util.UUID;

public interface ApplicationEventPublisherOutputPort {

    /**
     * Publish local spring event after payment intent created
     * @param intentId the payment intent identifier
     */
    void publishPaymentIntentCreated(UUID intentId);

    /**
     * Publish local spring event when payment processing starts
     * @param intentId the payment intent identifier
     */
    void publishPaymentProcessingStarted(UUID intentId);

    /**
     * Publish local spring event when 3DS is required
     * @param intentId the payment intent identifier
     * @param challengeUrl url for 3DS challenge
     */
    void publishThreeDSecureRequired(UUID intentId, String challengeUrl);
}