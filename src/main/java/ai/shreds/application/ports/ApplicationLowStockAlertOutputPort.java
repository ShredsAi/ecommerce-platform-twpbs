package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedLowStockAlertEvent;

public interface ApplicationLowStockAlertOutputPort {
    /**
     * Publishes a low stock alert event to notify stakeholders
     *
     * @param event The low stock alert event containing alert details
     */
    void publishLowStockAlert(SharedLowStockAlertEvent event);
}