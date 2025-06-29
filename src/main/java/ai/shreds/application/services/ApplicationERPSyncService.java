package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationERPSyncInputPort;
import ai.shreds.application.ports.ApplicationInventoryEventOutputPort;
import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.domain.ports.DomainInputPortERPSync;
import ai.shreds.shared.dtos.ERPAdjustmentItem;
import ai.shreds.shared.dtos.SharedERPUpdateMessage;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import ai.shreds.shared.value_objects.SharedCacheKey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationERPSyncService implements ApplicationERPSyncInputPort {

    private static final int BATCH_SIZE = 100;

    private final DomainInputPortERPSync domainERPSyncPort;
    private final ApplicationInventoryEventOutputPort inventoryEventPort;
    private final ApplicationCacheOutputPort cachePort;

    @Override
    @Transactional
    public void processERPBatch(SharedERPUpdateMessage message) {
        log.info("Processing ERP batch: {}, total adjustments: {}",
                message.getErpBatchId(), message.getAdjustments().size());

        try {
            processBatchInChunks(message.getErpBatchId(), message.getAdjustments());
        } catch (Exception e) {
            log.error("Failed to process ERP batch: {}", message.getErpBatchId(), e);
            throw e;
        }
    }

    private void processBatchInChunks(String batchId, List<ERPAdjustmentItem> adjustments) {
        for (int i = 0; i < adjustments.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, adjustments.size());
            List<ERPAdjustmentItem> chunk = adjustments.subList(i, end);

            processChunk(batchId, chunk);
        }
    }

    private void processChunk(String batchId, List<ERPAdjustmentItem> chunk) {
        try {
            // Process chunk through domain service
            domainERPSyncPort.reconcileStock(batchId, chunk);

            // Publish inventory changes and invalidate cache for each item
            for (ERPAdjustmentItem item : chunk) {
                try {
                    // Publish inventory change event
                    publishInventoryChangeEvent(item);

                    // Invalidate cache
                    invalidateCache(item.getSkuId(), item.getLocationId());
                } catch (Exception e) {
                    log.warn("Failed post-processing for SKU: {} at location: {}",
                            item.getSkuId(), item.getLocationId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process chunk of {} items from batch {}", chunk.size(), batchId, e);
            throw e;
        }
    }

    private void publishInventoryChangeEvent(ERPAdjustmentItem item) {
        SharedInventoryChangedEvent event = new SharedInventoryChangedEvent(
                "ERPSync",
                item.getSkuId(),
                item.getLocationId(),
                null, // Previous quantity not available in ERP message
                item.getNewQuantity(),
                "ERP"
        );

        try {
            inventoryEventPort.publishInventoryChange(event);
        } catch (Exception e) {
            log.warn("Failed to publish inventory change event for SKU: {} at location: {}", item.getSkuId(), item.getLocationId(), e);
        }
    }

    private void invalidateCache(String skuId, String locationId) {
        try {
            SharedCacheKey cacheKey = new SharedCacheKey("stock", skuId, locationId);
            cachePort.evict(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for SKU: {} at location: {}", skuId, locationId, e);
        }
    }
}