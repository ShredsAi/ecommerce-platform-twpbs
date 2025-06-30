package ai.shreds.domain.ports;

import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import java.util.List;

public interface DomainInputPortSafetyStockMonitor {
    List<SharedLowStockAlertEvent> evaluateStockLevels();
    void persistAlerts(List<SharedLowStockAlertEvent> events);
}
