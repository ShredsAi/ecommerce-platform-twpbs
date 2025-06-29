package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.application.ports.ApplicationStockValidationInputPort;
import ai.shreds.domain.ports.DomainInputPortStockValidation;
import ai.shreds.shared.value_objects.SharedStockValidationRequestEvent;
import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;
import ai.shreds.shared.value_objects.SharedCacheKey;
import ai.shreds.shared.dtos.SharedStockLevelDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationStockValidationService implements ApplicationStockValidationInputPort {

    private final DomainInputPortStockValidation domainValidationPort;
    private final ApplicationCacheOutputPort cachePort;

    @Override
    @Transactional(readOnly = true)
    public SharedStockValidationResponseEvent validateStock(SharedStockValidationRequestEvent event) {
        log.debug("Validating stock availability for SKU: {} at location: {}, quantity: {}",
                event.getSkuId(), event.getLocationId(), event.getRequestedQuantity());

        // Try cache first for quick validation
        SharedCacheKey cacheKey = new SharedCacheKey("stock", event.getSkuId(), event.getLocationId());
        SharedStockValidationResponseEvent response = validateFromCache(cacheKey, event);

        if (response != null) {
            log.debug("Validation completed using cached data");
            return response;
        }

        // Cache miss or invalid cache, delegate to domain service
        log.debug("Cache miss, performing validation through domain service");
        return domainValidationPort.validateAvailability(
                event.getSkuId(),
                event.getLocationId(),
                event.getRequestedQuantity()
        );
    }

    private SharedStockValidationResponseEvent validateFromCache(SharedCacheKey cacheKey, 
            SharedStockValidationRequestEvent event) {
        try {
            return cachePort.get(cacheKey)
                    .map(stockLevel -> createValidationResponse(stockLevel, event.getRequestedQuantity()))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error accessing cache during validation, falling back to database", e);
            return null;
        }
    }

    private SharedStockValidationResponseEvent createValidationResponse(SharedStockLevelDTO stockLevel, 
            java.math.BigDecimal requestedQuantity) {
        boolean isAvailable = stockLevel.getAvailable().compareTo(requestedQuantity) >= 0;
        return new SharedStockValidationResponseEvent(
                isAvailable,
                stockLevel.getAvailable(),
                requestedQuantity
        );
    }
}