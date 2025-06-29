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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DomainServiceSafetyStockMonitor implements DomainInputPortSafetyStockMonitor {
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
        
        ruleRepository.findAll().forEach(rule -> {
            stockLedgerRepository.findBySkuIdAndLocationId(
                    rule.getSkuId().getValue(),
                    rule.getLocationId().getValue()
            ).ifPresent(ledger -> {
                DomainValueQuantity available = ledger.calculateAvailable();
                if (checkAgainstRule(ledger, rule)) {
                    DomainEventLowStockDetected event = new DomainEventLowStockDetected(
                            UUID.randomUUID(),
                            ledger.getSkuId().getValue(),
                            ledger.getLocationId().getValue(),
                            available.getValue(),
                            rule.getMinQuantity().getValue(),
                            Instant.now()
                    );
                    alerts.add(event.toLowStockAlertEvent());
                }
            });
        });
        
        return alerts;
    }

    @Override
    public void generateAlerts() {
        List<SharedLowStockAlertEvent> events = evaluateStockLevels();
        
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

    private boolean checkAgainstRule(ai.shreds.domain.entities.DomainEntityStockLedger ledger, 
                                    ai.shreds.domain.entities.DomainEntitySafetyStockRule rule) {
        DomainValueQuantity available = ledger.calculateAvailable();
        return available.compareTo(rule.getMinQuantity()) < 0;
    }
}
