package ai.shreds.application.ports;

import ai.shreds.shared.value_objects.SharedReturnRequestParams;
import ai.shreds.shared.dtos.SharedReturnResponseDTO;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import java.util.List;

public interface ApplicationReturnInputPort {
    SharedReturnResponseDTO requestReturn(SharedReturnRequestParams params);
    SharedReturnResponseDTO getReturn(String returnId);
    List<SharedReturnResponseDTO> getReturnsByOrder(String orderId);
    SharedReturnResponseDTO updateReturnStatus(String returnId, SharedReturnStatusEnum status);
}