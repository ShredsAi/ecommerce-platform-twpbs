package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.domain.entities.DomainEntityStockAdjustmentAudit;
import ai.shreds.domain.entities.DomainEntityStockLedger;
import ai.shreds.domain.events.DomainEventStockAdjusted;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.domain.ports.DomainInputPortStockAdjustment;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.domain.ports.DomainOutputPortSKURepository;
import ai.shreds.domain.ports.DomainOutputPortLocationRepository;
import ai.shreds.domain.ports.DomainOutputPortStockAdjustmentAuditRepository;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedStockAdjustmentResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class DomainServiceStockAdjustment implements DomainInputPortStockAdjustment {
    private final DomainOutputPortStockLedgerRepository stockLedgerRepository;
    private final DomainOutputPortSKURepository skuRepository;
    private final DomainOutputPortLocationRepository locationRepository;
    private final DomainOutputPortStockAdjustmentAuditRepository auditRepository;
    private final DomainOutputPortOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public DomainServiceStockAdjustment(
            DomainOutputPortStockLedgerRepository stockLedgerRepository,
            DomainOutputPortSKURepository skuRepository,
            DomainOutputPortLocationRepository locationRepository,
            DomainOutputPortStockAdjustmentAuditRepository auditRepository,
            DomainOutputPortOutboxRepository outboxRepository) {
        this.stockLedgerRepository = stockLedgerRepository;
        this.skuRepository = skuRepository;
        this.locationRepository = locationRepository;
        this.auditRepository = auditRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SharedStockAdjustmentResponseDTO adjustStock(String skuId,
                                                       String locationId,
                                                       DomainValueQuantityAdjustment adjustment) {
        // Validate SKU and Location exist and are active
        if (!skuRepository.existsAndActive(skuId)) {
            throw new DomainExceptionEntityNotFound("SKU", skuId);
        }
        if (!locationRepository.existsAndActive(locationId)) {
            throw new DomainExceptionEntityNotFound("Location", locationId);
        }

        // Find or create stock ledger
        DomainEntityStockLedger ledger = stockLedgerRepository.findBySkuIdAndLocationId(skuId, locationId)
                .orElseThrow(() -> new DomainExceptionEntityNotFound("StockLedger", skuId + ":" + locationId));

        // Apply adjustment
        DomainEventStockAdjusted event = ledger.adjustQuantity(adjustment);
        
        // Save updated ledger
        DomainEntityStockLedger savedLedger = stockLedgerRepository.save(ledger);

        // Create audit record
        DomainEntityStockAdjustmentAudit audit = createAuditRecord(savedLedger, adjustment, event);
        auditRepository.save(audit);

        // Create and save outbox event
        DomainEntityOutboxEvent outboxEvent = createOutboxEvent(event);
        outboxRepository.save(outboxEvent);

        return new SharedStockAdjustmentResponseDTO(
                "SUCCESS",
                savedLedger.getLedgerId().getValue().toString(),
                event.getNewQuantity(),
                Instant.now());
    }

    private DomainEntityStockAdjustmentAudit createAuditRecord(DomainEntityStockLedger ledger, 
                                                              DomainValueQuantityAdjustment adjustment, 
                                                              DomainEventStockAdjusted event) {
        return DomainEntityStockAdjustmentAudit.create(
                ledger.getLedgerId(),
                ledger.getSkuId(),
                ledger.getLocationId(),
                adjustment.getAdjustment().getValue(),
                event.getPreviousQuantity(),
                event.getNewQuantity(),
                adjustment.getReason(),
                "DOMAIN",
                null
        );
    }

    private DomainEntityOutboxEvent createOutboxEvent(DomainEventStockAdjusted event) {
        try {
            String payload = objectMapper.writeValueAsString(event.toInventoryChangedEvent());
            return DomainEntityOutboxEvent.create(
                    event.getLedgerId(),
                    "STOCK_LEDGER",
                    "StockAdjusted",
                    payload
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }
}
