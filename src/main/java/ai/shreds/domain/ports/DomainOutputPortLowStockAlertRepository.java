package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityLowStockAlert;
import java.util.Optional;
import java.util.UUID;

public interface DomainOutputPortLowStockAlertRepository {
    Optional<DomainEntityLowStockAlert> findUnresolvedBySkuIdAndLocationId(String skuId, String locationId);
    DomainEntityLowStockAlert save(DomainEntityLowStockAlert alert);
    void updateStatus(UUID alertId, String status);
}
