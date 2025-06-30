package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityStockAdjustmentAudit;
import java.util.List;

public interface DomainOutputPortStockAdjustmentAuditRepository {
    void save(DomainEntityStockAdjustmentAudit audit);
    void saveAll(List<DomainEntityStockAdjustmentAudit> audits);
}
