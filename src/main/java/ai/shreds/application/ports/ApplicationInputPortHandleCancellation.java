package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationOrderCancelledDTO;
import ai.shreds.application.dtos.ApplicationCancellationResultDTO;

/**
 * Input port for handling order cancellation requests.
 */
public interface ApplicationInputPortHandleCancellation {

    /**
     * Processes a cancellation request.
     *
     * @param cancellation DTO containing cancellation request details
     * @return result of the cancellation handling, including statuses and messages
     */
    ApplicationCancellationResultDTO handleCancellation(ApplicationOrderCancelledDTO cancellation);
}
