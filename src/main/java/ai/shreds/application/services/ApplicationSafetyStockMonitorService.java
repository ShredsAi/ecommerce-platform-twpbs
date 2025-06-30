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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public void checkAndGenerateAlerts() {
        log.info("Starting safety stock level check");
        performSafetyStockCheck();
    }
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    @ConditionalOnProperty(value = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    @Transactional
    public void scheduledCheckAndGenerateAlerts() {
        log.info("Starting scheduled safety stock level check");
        performSafetyStockCheck();
    }
    
    private void performSafetyStockCheck() {
        try {
            // First evaluate stock levels
            List<SharedLowStockAlertEvent> alerts = domainMonitorPort.evaluateStockLevels();
            
            // Then persist any alerts
            if (!alerts.isEmpty()) {
                log.info("Found {} low stock conditions", alerts.size());
                domainMonitorPort.persistAlerts(alerts);
            } else {
                log.debug("No low stock conditions detected");
            }
        } catch (Exception e) {
            log.error("Error during safety stock monitoring", e);
            throw e;
        }
    }
}
