package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationStockValidationInputPort;
import ai.shreds.shared.value_objects.SharedStockValidationRequestEvent;
import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Listens for stock validation requests and publishes validation responses.
 * This adapter connects the intra-service event infrastructure to the application layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterStockValidationEventListener {

    private final ApplicationStockValidationInputPort applicationValidationPort;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    /**
     * Handles stock validation request events asynchronously.
     * Validates the requested stock quantity and publishes the validation result.
     *
     * @param event The stock validation request event
     */
    @EventListener
    @Async("stockValidationExecutor")
    @Transactional(readOnly = true)
    public void onStockValidationRequest(SharedStockValidationRequestEvent event) {
        String correlationId = java.util.UUID.randomUUID().toString();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        try {
            log.debug("[{}] Processing stock validation request for SKU: {}, location: {}, quantity: {}", 
                    correlationId, event.getSkuId(), event.getLocationId(), event.getRequestedQuantity());
            
            // Increment counter for monitoring request volume
            meterRegistry.counter("stock.validation.requests", 
                    "sku", event.getSkuId(),
                    "location", event.getLocationId()).increment();
            
            // Call application service to validate the stock
            SharedStockValidationResponseEvent response = applicationValidationPort.validateStock(event);
            
            // Record validation result metrics
            meterRegistry.counter("stock.validation.results", 
                    "sku", event.getSkuId(),
                    "location", event.getLocationId(),
                    "available", response.getIsAvailable().toString()).increment();
            
            // Publish the response event
            eventPublisher.publishEvent(response);
            
            log.debug("[{}] Published stock validation response: available={}, availableQty={}, requestedQty={}", 
                    correlationId, response.getIsAvailable(), response.getAvailableQuantity(), response.getRequestedQuantity());
            
        } catch (Exception e) {
            // Record exception for monitoring
            meterRegistry.counter("stock.validation.errors", 
                    "sku", event.getSkuId(),
                    "location", event.getLocationId(),
                    "error", e.getClass().getSimpleName()).increment();
            
            log.error("[{}] Error validating stock for SKU: {}, location: {}", 
                    correlationId, event.getSkuId(), event.getLocationId(), e);
            
            // Create error response - stock is not available when error occurs
            SharedStockValidationResponseEvent errorResponse = new SharedStockValidationResponseEvent(
                    event.getSkuId(),
                    event.getLocationId(),
                    false, // Not available when error occurs
                    null,  // Unknown available quantity
                    event.getRequestedQuantity());
            
            // Publish error response
            eventPublisher.publishEvent(errorResponse);
            
        } finally {
            // Record the total processing time
            timerSample.stop(Timer.builder("stock.validation.time")
                    .description("Time to process stock validation requests")
                    .tag("sku", event.getSkuId())
                    .tag("location", event.getLocationId())
                    .register(meterRegistry));
        }
    }
}