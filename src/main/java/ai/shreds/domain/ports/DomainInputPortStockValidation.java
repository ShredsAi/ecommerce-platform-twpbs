package ai.shreds.domain.ports;

import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;
import java.math.BigDecimal;

public interface DomainInputPortStockValidation {
    SharedStockValidationResponseEvent validateAvailability(String skuId, String locationId, BigDecimal requestedQuantity);
}