package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationOrderCreationRequestDTO;
import ai.shreds.application.dtos.ApplicationOrderCreationResponseDTO;

public interface ApplicationCreateOrderInputPort {
    ApplicationOrderCreationResponseDTO execute(ApplicationOrderCreationRequestDTO request);
}
