package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationShippingUpdateDTO;
import ai.shreds.application.dtos.ApplicationShippingUpdateResultDTO;

/**
 * Input port for handling shipping updates.
 */
public interface ApplicationInputPortHandleShippingUpdate {

    /**
     * Processes a shipping update event.
     *
     * @param update DTO containing shipping update details
     * @return result of processing the shipping update
     */
    ApplicationShippingUpdateResultDTO handleShippingUpdate(ApplicationShippingUpdateDTO update);
}
