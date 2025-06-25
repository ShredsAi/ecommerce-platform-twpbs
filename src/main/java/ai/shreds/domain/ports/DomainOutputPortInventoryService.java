package ai.shreds.domain.ports;

import ai.shreds.application.dtos.ApplicationInventoryCheckResponseDTO;
import ai.shreds.application.dtos.ApplicationInventoryItemDTO;

import java.util.List;

/**
 * Domain output port for inventory service integration.
 * This port is implemented by infrastructure layer.
 */
public interface DomainOutputPortInventoryService {
    
    /**
     * Verifies inventory availability for the given items.
     *
     * @param items the list of items to check availability for
     * @return the availability check response
     */
    ApplicationInventoryCheckResponseDTO verifyAvailability(List<ApplicationInventoryItemDTO> items);
}