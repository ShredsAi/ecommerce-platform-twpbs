package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityStockLedger;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.domain.ports.DomainInputPortStockValidation;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.domain.value_objects.DomainValueQuantity;
import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DomainServiceStockValidation implements DomainInputPortStockValidation {
    private final DomainOutputPortStockLedgerRepository stockLedgerRepository;

    public DomainServiceStockValidation(DomainOutputPortStockLedgerRepository stockLedgerRepository) {
        this.stockLedgerRepository = stockLedgerRepository;
    }

    @Override
    public SharedStockValidationResponseEvent validateAvailability(
            String skuId,
            String locationId,
            BigDecimal requestedQuantity) {
        
        // Find the stock ledger or throw exception if not found
        DomainEntityStockLedger ledger = stockLedgerRepository.findBySkuIdAndLocationId(skuId, locationId)
                .orElseThrow(() -> new DomainExceptionEntityNotFound("StockLedger", skuId + ":" + locationId));
        
        // Calculate available quantity using domain logic
        DomainValueQuantity available = ledger.calculateAvailable();
        DomainValueQuantity requested = new DomainValueQuantity(requestedQuantity);
        
        // Determine if requested quantity is available
        boolean isAvailable = available.compareTo(requested) >= 0;
        
        // Create and return validation response event with all required fields
        return new SharedStockValidationResponseEvent(
                skuId,
                locationId,
                isAvailable, 
                available.getValue(), 
                requestedQuantity
        );
    }
}
