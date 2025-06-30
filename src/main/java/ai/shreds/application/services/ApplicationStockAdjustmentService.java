package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationExceptionOptimisticLockException;
import ai.shreds.application.ports.*;
import ai.shreds.domain.ports.DomainInputPortStockAdjustment;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.domain.value_objects.DomainValueQuantity;
import ai.shreds.domain.value_objects.DomainValueQuantityAdjustment;
import ai.shreds.shared.dtos.*;
import ai.shreds.shared.value_objects.SharedCacheKey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationStockAdjustmentService implements ApplicationStockAdjustmentInputPort {

    private final DomainInputPortStockAdjustment domainStockAdjustmentPort;
    private final DomainOutputPortOutboxRepository domainOutboxPort;
    private final ApplicationInventoryEventOutputPort inventoryEventPort;
    private final ApplicationInventoryChangeNotificationOutputPort notificationPort;
    private final ApplicationCacheOutputPort cachePort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SharedStockAdjustmentResponseDTO adjustStock(SharedStockAdjustmentRequestDTO request) {
        log.info("Processing stock adjustment request for SKU: {} at location: {}", 
                request.getSkuId(), request.getLocationId());

        try {
            // Create domain value objects for adjustment
            DomainValueQuantity quantityAdjustment = new DomainValueQuantity(request.getAdjustment());
            DomainValueQuantityAdjustment adjustment = new DomainValueQuantityAdjustment(
                    quantityAdjustment,
                    request.getReason(),
                    "Stock adjustment via API"
            );

            // Perform adjustment through domain service
            SharedStockAdjustmentResponseDTO response = domainStockAdjustmentPort.adjustStock(
                    request.getSkuId(),
                    request.getLocationId(),
                    adjustment
            );

            // Create and save domain outbox event
            SharedInventoryChangedEvent event = new SharedInventoryChangedEvent(
                    "StockAdjusted",
                    request.getSkuId(),
                    request.getLocationId(),
                    response.getNewQuantity().subtract(request.getAdjustment()),
                    response.getNewQuantity(),
                    "API"
            );

            // Serialize payload
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                log.error("Error serializing inventory event: {}", e.getMessage());
                payload = "{\"error\":\"Serialization failed\", \"eventType\":\"" + 
                         event.getEventType() + "\", \"skuId\":\"" + event.getSkuId() + "\"}";
            }

            // Create DomainEntityOutboxEvent
            UUID aggId = UUID.fromString(response.getLedgerId());
            DomainEntityOutboxEvent outboxEntity = DomainEntityOutboxEvent.create(
                    aggId,
                    "STOCK_LEDGER",
                    "STOCK_ADJUSTED",
                    payload
            );
            domainOutboxPort.save(outboxEntity);

            // Publish events and notifications
            inventoryEventPort.publishInventoryChange(event);
            notificationPort.sendInventoryChangeNotification(new SharedInventoryChangeMessage(
                    request.getSkuId(),
                    request.getLocationId(),
                    response.getNewQuantity().subtract(request.getAdjustment()),
                    response.getNewQuantity()
            ));

            // Invalidate cache
            invalidateCache(request.getSkuId(), request.getLocationId());

            return response;

        } catch (Exception e) {
            if (isOptimisticLockException(e)) {
                throw new ApplicationExceptionOptimisticLockException(
                        "Concurrent modification detected while adjusting stock", e);
            }
            throw e;
        }
    }

    private void invalidateCache(String skuId, String locationId) {
        try {
            SharedCacheKey cacheKey = new SharedCacheKey("stock", skuId, locationId);
            cachePort.evict(cacheKey);
            log.debug("Invalidated cache for SKU: {} at location: {}", skuId, locationId);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for SKU: {} at location: {}", skuId, locationId, e);
        }
    }

    private boolean isOptimisticLockException(Exception e) {
        return e.getCause() != null && 
               e.getCause().getClass().getSimpleName().contains("OptimisticLockingFailureException");
    }
}
