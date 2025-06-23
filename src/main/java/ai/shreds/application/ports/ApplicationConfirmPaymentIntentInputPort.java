package ai.shreds.application.ports;

import java.util.UUID;
import ai.shreds.application.dtos.ApplicationConfirmPaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationPaymentConfirmationDTO;

public interface ApplicationConfirmPaymentIntentInputPort {

    /**
     * Confirm an existing payment intent
     *
     * @param id  the payment intent identifier
     * @param dto confirmation data containing clientSecret
     * @return confirmation result with status and actions
     */
    ApplicationPaymentConfirmationDTO confirmPaymentIntent(UUID id, ApplicationConfirmPaymentIntentDTO dto);
}