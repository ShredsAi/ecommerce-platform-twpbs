package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityLowStockAlert;
import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.domain.events.DomainEventLowStockDetected;
import ai.shreds.domain.ports.DomainInputPortSafetyStockMonitor;
import ai.shreds.domain.ports.DomainOutputPortLowStockAlertRepository;
import ai.shreds.domain.ports.DomainOutputPortSafetyStockRuleRepository;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DomainServiceSafetyStockMonitor implements DomainInputPortSafetyStockMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(DomainServiceSafetyStockMonitor.class);
    
    private final DomainOutputPortStockLedgerRepository stockLedgerRepository;
    private final DomainOutputPortSafetyStockRuleRepository ruleRepository;
    private final DomainOutputPortLowStockAlertRepository alertRepository;
    private final DomainOutputPortOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public DomainServiceSafetyStockMonitor(
            DomainOutputPortStockLedgerRepository stockLedgerRepository,
            DomainOutputPortSafetyStockRuleRepository ruleRepository,
            DomainOutputPortLowStockAlertRepository alertRepository,
            DomainOutputPortOutboxRepository outboxRepository) {
        this.stockLedgerRepository = stockLedgerRepository;
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<SharedLowStockAlertEvent> evaluateStockLevels() {
        List<SharedLowStockAlertEvent> alerts = new ArrayList<>();
        
        log.debug("[SAFETY-MONITOR] Starting stock level evaluation");
        
        List<ai.shreds.domain.entities.DomainEntitySafetyStockRule> allRules = ruleRepository.findAll();
        log.debug("[SAFETY-MONITOR] Found {} safety stock rules", allRules.size());
        
        allRules.forEach(rule -> {
            String skuId = rule.getSkuId().getValue();
            String locationId = rule.getLocationId().getValue();
            log.debug("[SAFETY-MONITOR] Evaluating rule for SKU: {} at location: {}, min quantity: {}", 
                     skuId, locationId, rule.getMinQuantity().getValue());
            
            stockLedgerRepository.findBySkuIdAndLocationId(skuId, locationId)
                .ifPresentOrElse(
                    ledger -> {
                        DomainValueQuantity available = ledger.calculateAvailable();
                        log.debug("[SAFETY-MONITOR] Found stock ledger - available: {}, threshold: {}", 
                                 available.getValue(), rule.getMinQuantity().getValue());
                        
                        if (checkAgainstRule(ledger, rule)) {
                            log.info("[SAFETY-MONITOR] LOW STOCK DETECTED for SKU: {} at location: {}, available: {}, threshold: {}",
                                    skuId, locationId, available.getValue(), rule.getMinQuantity().getValue());
                            
                            DomainEventLowStockDetected event = new DomainEventLowStockDetected(
                                    UUID.randomUUID(),
                                    ledger.getSkuId().getValue(),
                                    ledger.getLocationId().getValue(),
                                    available.getValue(),
                                    rule.getMinQuantity().getValue(),
                                    Instant.now()
                            );
                            alerts.add(event.toLowStockAlertEvent());
                        } else {
                            log.debug("[SAFETY-MONITOR] Stock level OK for SKU: {} at location: {}, available: {} >= threshold: {}",
                                     skuId, locationId, available.getValue(), rule.getMinQuantity().getValue());
                        }
                    },
                    () -> log.warn("[SAFETY-MONITOR] No stock ledger found for SKU: {} at location: {}", skuId, locationId)
                );
        });
        
        log.debug("[SAFETY-MONITOR] Stock level evaluation completed - {} alerts generated", alerts.size());
        return alerts;
    }

    @Override
    public void persistAlerts(List<SharedLowStockAlertEvent> events) {
        for (SharedLowStockAlertEvent alertEvent : events) {
            // Check if alert already exists for this SKU and location
            if (alertRepository.findUnresolvedBySkuIdAndLocationId(
                    alertEvent.getSkuId(), 
                    alertEvent.getLocationId()).isEmpty()) {
                
                // Create new alert entity
                DomainEntityLowStockAlert alert = DomainEntityLowStockAlert.create(
                        new DomainValueSkuId(alertEvent.getSkuId()),
                        new DomainValueLocationId(alertEvent.getLocationId()),
                        new DomainValueRuleId(UUID.randomUUID()), // We need to get actual rule ID
                        new DomainValueQuantity(alertEvent.getCurrentQuantity()),
                        new DomainValueQuantity(alertEvent.getThreshold())
                );
                
                DomainEntityLowStockAlert savedAlert = alertRepository.save(alert);
                
                // Create outbox event for external notification
                try {
                    String payload = objectMapper.writeValueAsString(alertEvent);
                    DomainEntityOutboxEvent outboxEvent = DomainEntityOutboxEvent.create(
                            savedAlert.getAlertId().getValue(),
                            "LOW_STOCK_ALERT",
                            "LowStockDetected",
                            payload
                    );
                    outboxRepository.save(outboxEvent);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize alert event to JSON", e);
                }
            }
        }
    }

    public void generateAlerts() {
        List<SharedLowStockAlertEvent> events = evaluateStockLevels();
        persistAlerts(events);
    }

    private boolean checkAgainstRule(ai.shreds.domain.entities.DomainEntityStockLedger ledger, 
                                    ai.shreds.domain.entities.DomainEntitySafetyStockRule rule) {
        DomainValueQuantity available = ledger.calculateAvailable();
        boolean isLowStock = available.compareTo(rule.getMinQuantity()) < 0;
        log.debug("[SAFETY-MONITOR] Comparison - available: {} < threshold: {} = {}", 
                 available.getValue(), rule.getMinQuantity().getValue(), isLowStock);
        return isLowStock;
    }
}
