package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationInventoryCheckRequestDTO;
import ai.shreds.application.dtos.ApplicationInventoryCheckResponseDTO;

public interface ApplicationInventoryOutputPort {
    ApplicationInventoryCheckResponseDTO checkAvailability(ApplicationInventoryCheckRequestDTO request);
}
