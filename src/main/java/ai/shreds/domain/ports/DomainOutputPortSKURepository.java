package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntitySKU;
import java.util.Optional;

public interface DomainOutputPortSKURepository {
    Optional<DomainEntitySKU> findById(String skuId);
    boolean existsAndActive(String skuId);
}
