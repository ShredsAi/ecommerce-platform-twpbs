package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityERPReconciliation;
import java.util.Optional;

public interface DomainOutputPortERPReconciliationRepository {
    DomainEntityERPReconciliation save(DomainEntityERPReconciliation reconciliation);
    Optional<DomainEntityERPReconciliation> findByBatchId(String batchId);
}
