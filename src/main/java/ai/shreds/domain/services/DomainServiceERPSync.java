package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityERPReconciliation;
import ai.shreds.domain.entities.DomainEntityStockAdjustmentAudit;
import ai.shreds.domain.entities.DomainEntityStockLedger;
import ai.shreds.domain.events.DomainEventStockAdjusted;
import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.ports.DomainInputPortERPSync;
import ai.shreds.domain.ports.DomainOutputPortERPReconciliationRepository;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.domain.ports.DomainOutputPortStockAdjustmentAuditRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedERPUpdateMessage;
import ai.shreds.shared.dtos.ERPAdjustmentItem;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class DomainServiceERPSync implements DomainInputPortERPSync {
    private final DomainOutputPortERPReconciliationRepository reconciliationRepository;
    private final DomainOutputPortStockLedgerRepository stockLedgerRepository;
    private final DomainOutputPortStockAdjustmentAuditRepository auditRepository;

    public DomainServiceERPSync(
            DomainOutputPortERPReconciliationRepository reconciliationRepository,
            DomainOutputPortStockLedgerRepository stockLedgerRepository,
            DomainOutputPortStockAdjustmentAuditRepository auditRepository) {
        this.reconciliationRepository = reconciliationRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void reconcileStock(String batchId, List<ERPAdjustmentItem> adjustments) {
        // Prevent duplicate batch processing
        Optional<DomainEntityERPReconciliation> existing = reconciliationRepository.findByBatchId(batchId);
        if (existing.isPresent()) {
            throw new DomainExceptionInvalidState("RECONCILIATION", "batch " + batchId + " already processed");
        }

        // Create and start reconciliation
        DomainEntityERPReconciliation reconciliation = DomainEntityERPReconciliation.create(batchId, adjustments.size());
        reconciliation.start();

        // Process each adjustment
        for (ERPAdjustmentItem item : adjustments) {
            try {
                processAdjustment(item, reconciliation);
            } catch (Exception ex) {
                reconciliation.recordError(new DomainValueReconciliationError(
                        item.getSkuId(), 
                        item.getLocationId(), 
                        ex.getMessage(), 
                        "ERR"
                ));
            }
        }

        // Complete or fail based on processing results
        if (reconciliation.isComplete()) {
            reconciliation.complete();
        } else {
            reconciliation.fail("Processing incomplete or errors occurred");
        }

        reconciliationRepository.save(reconciliation);
    }

    private void processAdjustment(ERPAdjustmentItem item,
                                   DomainEntityERPReconciliation reconciliation) {
        String skuId = item.getSkuId();
        String locationId = item.getLocationId();
        BigDecimal newQuantity = item.getNewQuantity();

        // Find existing ledger or create new one
        DomainEntityStockLedger ledger = stockLedgerRepository.findBySkuIdAndLocationId(skuId, locationId)
                .orElseGet(() -> DomainEntityStockLedger.create(
                        new DomainValueSkuId(skuId),
                        new DomainValueLocationId(locationId),
                        new DomainValueQuantity(BigDecimal.ZERO)
                ));

        // Calculate adjustment delta
        BigDecimal previousQuantity = ledger.getQuantity().getValue();
        BigDecimal deltaQuantity = newQuantity.subtract(previousQuantity);
        
        // Only process if there's an actual change
        if (deltaQuantity.compareTo(BigDecimal.ZERO) != 0) {
            DomainValueQuantityAdjustment adjustment = new DomainValueQuantityAdjustment(
                    new DomainValueQuantity(deltaQuantity),
                    SharedEnumAdjustmentReason.ERP_SYNC,
                    "ERP batch " + reconciliation.getBatchId().getValue()
            );

            // Apply adjustment
            DomainEventStockAdjusted event = ledger.adjustQuantity(adjustment);
            stockLedgerRepository.save(ledger);

            // Create audit record
            DomainEntityStockAdjustmentAudit audit = DomainEntityStockAdjustmentAudit.create(
                    ledger.getLedgerId(),
                    new DomainValueSkuId(skuId),
                    new DomainValueLocationId(locationId),
                    event.getAdjustment(),
                    event.getPreviousQuantity(),
                    event.getNewQuantity(),
                    event.getReason(),
                    "ERP_SYNC",
                    null
            );
            auditRepository.save(audit);
        }
        
        reconciliation.recordSuccess();
    }
}