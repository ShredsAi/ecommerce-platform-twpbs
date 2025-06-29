package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationSafetyStockMonitorInputPort;
import ai.shreds.application.ports.ApplicationLowStockAlertOutputPort;
import ai.shreds.domain.ports.DomainInputPortSafetyStockMonitor;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSafetyStockMonitorService implements ApplicationSafetyStockMonitorInputPort {

    private final DomainInputPortSafetyStockMonitor domainMonitorPort;
    private final ApplicationLowStockAlertOutputPort alertPort;
    private final DomainOutputPortOutboxRepository domainOutboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkAndGenerateAlerts() {
        log.info("Starting scheduled safety stock level check");

        try {
            List<SharedLowStockAlertEvent> alerts = domainMonitorPort.evaluateStockLevels();

            if (!alerts.isEmpty()) {
                log.info("Found {} low stock conditions", alerts.size());
                publishAlerts(alerts);
            } else {
                log.debug("No low stock conditions detected");
            }

        } catch (Exception e) {
            log.error("Error during safety stock monitoring", e);
            throw e;
        }
    }

    private void publishAlerts(List<SharedLowStockAlertEvent> alerts) {
        for (SharedLowStockAlertEvent alert : alerts) {
            try {
                // Serialize alert to JSON
                String payload;
                try {
                    payload = objectMapper.writeValueAsString(alert);
                } catch (Exception e) {
                    log.error("Error serializing low stock alert: {}", e.getMessage());
                    payload = "{\"error\":\"Serialization failed\", \"alertId\":\"" 
                            + alert.getAlertId() + "\", \"skuId\":\"" + alert.getSkuId() + "\"}";
                }

                // Create domain outbox event
                UUID aggId = UUID.fromString(alert.getAlertId());
                DomainEntityOutboxEvent outboxEntity = DomainEntityOutboxEvent.create(
                        aggId,
                        "LOW_STOCK_ALERT",
                        "LOW_STOCK_DETECTED",
                        payload
                );
                domainOutboxPort.save(outboxEntity);

                // Publish alert
                alertPort.publishLowStockAlert(alert);

                log.debug("Published low stock alert for SKU: {} at location: {}", alert.getSkuId(), alert.getLocationId());

            } catch (Exception e) {
                log.error("Failed to publish low stock alert for SKU: {} at location: {}", alert.getSkuId(), alert.getLocationId(), e);
            }
        }
    }
}
