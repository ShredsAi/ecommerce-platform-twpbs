package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntitySafetyStockRule;
import java.util.List;
import java.util.Optional;

public interface DomainOutputPortSafetyStockRuleRepository {
    Optional<DomainEntitySafetyStockRule> findActiveBySkuIdAndLocationId(String skuId, String locationId);
    DomainEntitySafetyStockRule save(DomainEntitySafetyStockRule rule);
    List<DomainEntitySafetyStockRule> findAll();
    void deactivateAllBySkuId(String skuId);
}
