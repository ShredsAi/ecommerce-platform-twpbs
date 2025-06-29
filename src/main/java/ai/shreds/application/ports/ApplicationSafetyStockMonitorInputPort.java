package ai.shreds.application.ports;

public interface ApplicationSafetyStockMonitorInputPort {
    /**
     * Checks all active safety stock rules against current stock levels
     * and generates alerts for any breaches detected
     */
    void checkAndGenerateAlerts();
}