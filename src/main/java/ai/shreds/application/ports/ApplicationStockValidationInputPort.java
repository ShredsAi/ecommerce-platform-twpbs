package ai.shreds.application.ports;

import ai.shreds.shared.value_objects.SharedStockValidationRequestEvent;
import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;

public interface ApplicationStockValidationInputPort {
    /**
     * Validates if requested stock quantity is available for reservation
     *
     * @param event The validation request event containing SKU, location and requested quantity
     * @return Response event indicating if stock is available
     */
    SharedStockValidationResponseEvent validateStock(SharedStockValidationRequestEvent event);
}