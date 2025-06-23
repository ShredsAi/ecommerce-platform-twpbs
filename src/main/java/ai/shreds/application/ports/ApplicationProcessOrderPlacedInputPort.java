package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationOrderPlacedDTO;

public interface ApplicationProcessOrderPlacedInputPort {

    /**
     * Process an OrderPlaced event from Kafka to auto-generate PaymentIntent
     * @param dto data from OrderPlaced event
     */
    void processOrderPlaced(ApplicationOrderPlacedDTO dto);
}