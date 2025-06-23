package ai.shreds.application.ports;

import java.util.UUID;
import ai.shreds.application.dtos.ApplicationPaymentDetailsDTO;

public interface ApplicationGetPaymentInputPort {

    /**
     * Retrieve details of a processed payment
     *
     * @param id payment identifier
     * @return payment details DTO
     */
    ApplicationPaymentDetailsDTO getPayment(UUID id);
}