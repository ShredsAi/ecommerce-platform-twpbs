package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationExceptionStockNotFoundException;
import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.application.ports.ApplicationStockQueryInputPort;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.shared.dtos.SharedStockLevelDTO;
import ai.shreds.shared.value_objects.SharedCacheKey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationStockQueryService implements ApplicationStockQueryInputPort {

    private final DomainOutputPortStockLedgerRepository domainStockLedgerPort;
    private final ApplicationCacheOutputPort cachePort;

    @Override
    @Transactional(readOnly = true)
    public SharedStockLevelDTO getStock(String skuId, String locationId) {
        log.debug("Getting stock for SKU: {} at location: {}", skuId, locationId);
        
        // Create cache key
        SharedCacheKey cacheKey = new SharedCacheKey("stock", skuId, locationId);
        
        // Check cache first
        Optional<SharedStockLevelDTO> cachedStock = cachePort.get(cacheKey);
        
        if (cachedStock.isPresent()) {
            log.debug("Cache hit for SKU: {} at location: {}", skuId, locationId);
            return cachedStock.get();
        }
        
        log.debug("Cache miss for SKU: {} at location: {}, loading from database", skuId, locationId);
        SharedStockLevelDTO stockLevelDTO = loadFromDatabase(skuId, locationId);
        
        // Update cache
        updateCache(cacheKey, stockLevelDTO);
        
        return stockLevelDTO;
    }
    
    private SharedStockLevelDTO loadFromDatabase(String skuId, String locationId) {
        return domainStockLedgerPort.findBySkuIdAndLocationId(skuId, locationId)
                .map(stockLedger -> stockLedger.toDTO())
                .orElseThrow(() -> new ApplicationExceptionStockNotFoundException(skuId, locationId));
    }
    
    private void updateCache(SharedCacheKey key, SharedStockLevelDTO dto) {
        try {
            cachePort.put(key, dto);
            log.debug("Updated cache for key: {}", key.toKey());
        } catch (Exception e) {
            // Log but don't fail if cache update fails
            log.warn("Failed to update cache for key: {}, error: {}", key.toKey(), e.getMessage());
        }
    }
}