package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationCreatePaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationPaymentIntentDTO;

public interface ApplicationCreatePaymentIntentInputPort {

    /**
     * Create a new payment intent based on client request
     * @param dto request data
     * @return created payment intent details
     */
    ApplicationPaymentIntentDTO createPaymentIntent(ApplicationCreatePaymentIntentDTO dto);
}