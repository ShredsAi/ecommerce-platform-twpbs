package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationOrderCreatedDTO;
import ai.shreds.application.dtos.ApplicationSagaStartResultDTO;

/**
 * Input port for starting a new saga based on an order creation event.
 */
public interface ApplicationInputPortStartSaga {

    /**
     * Initiates the order fulfillment saga.
     *
     * @param orderCreated DTO containing order creation details
     * @return result of the saga start, including sagaId and next step
     */
    ApplicationSagaStartResultDTO startSaga(ApplicationOrderCreatedDTO orderCreated);
}
