package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedStockLevelDTO;

public interface ApplicationStockQueryInputPort {
    /**
     * Retrieves current stock level information for a specific SKU at a specific location
     *
     * @param skuId The SKU identifier
     * @param locationId The location identifier
     * @return Current stock level information
     */
    SharedStockLevelDTO getStock(String skuId, String locationId);
}