package ai.shreds.application.ports;

import ai.shreds.shared.value_objects.SharedCancellationRequestParams;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.dtos.SharedSystemCancellationMessage;
import java.util.List;

public interface ApplicationCancellationInputPort {

    SharedCancellationResponseDTO requestCancellation(SharedCancellationRequestParams params);

    SharedCancellationResponseDTO getCancellation(String cancellationId);

    List<SharedCancellationResponseDTO> getCancellationsByOrder(String orderId);

    void processSystemCancellation(SharedSystemCancellationMessage message);

    void completeCancellation(String cancellationId);
}
