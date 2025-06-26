package ai.shreds.application.ports;

import java.util.List;
import ai.shreds.shared.dtos.SharedSystemCancellationMessage;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.value_objects.SharedCancellationRequestParams;

/**
 * Input port for handling cancellation requests and system cancellation messages.
 */
public interface ApplicationCancellationInputPort {

    /**
     * Handles a cancellation request from client.
     */
    SharedCancellationResponseDTO requestCancellation(SharedCancellationRequestParams params);

    /**
     * Retrieves a cancellation by its ID.
     */
    SharedCancellationResponseDTO getCancellation(String cancellationId);

    /**
     * Retrieves cancellations for a given order.
     */
    List<SharedCancellationResponseDTO> getCancellationsByOrder(String orderId);

    /**
     * Processes a system-originated cancellation message.
     */
    void processSystemCancellation(SharedSystemCancellationMessage message);
}