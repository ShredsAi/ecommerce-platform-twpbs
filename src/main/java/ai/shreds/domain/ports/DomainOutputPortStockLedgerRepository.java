package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityStockLedger;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface DomainOutputPortStockLedgerRepository {
    Optional<DomainEntityStockLedger> findBySkuIdAndLocationId(String skuId, String locationId);
    DomainEntityStockLedger save(DomainEntityStockLedger stockLedger);
    void updateQuantityAtomic(UUID ledgerId, BigDecimal deltaQuantity);
    DomainEntityStockLedger lockById(UUID ledgerId);
}
