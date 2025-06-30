package ai.shreds.adapter.primary;

import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for publishing inventory change events.
 * Sends events about inventory changes to downstream systems for analytics and dashboards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterInventoryEventKafkaProducer {

    @Value("${app.kafka.events.inventory.topic:inventory.events}")
    private String topic;
    
    private final KafkaTemplate<String, SharedInventoryChangedEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Publishes an inventory changed event to Kafka.
     * Uses retryable mechanism to handle transient errors.
     * 
     * @param event The inventory changed event to publish
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public void publishInventoryEvent(SharedInventoryChangedEvent event) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        String key = event.getSkuId() + "|" + event.getLocationId();
        String correlationId = UUID.randomUUID().toString();

        try {
            log.debug("[INV-EVENT] Publishing inventory event: {} for SKU: {}, Location: {} (Correlation: {})", 
                      event.getEventType(), event.getSkuId(), event.getLocationId(), correlationId);

            validateEvent(event);

            CompletableFuture<SendResult<String, SharedInventoryChangedEvent>> future =
                    kafkaTemplate.send(topic, key, event);
            
            future.handle((result, ex) -> {
                if (result != null) {
                    recordSuccessMetrics(event);
                    log.debug("[INV-EVENT] Successfully published inventory event: {} (Correlation: {})", 
                            event.getEventType(), correlationId);
                } else {
                    recordFailureMetrics(event, ex);
                    log.error("[INV-EVENT] Failed to publish inventory event: {} (Correlation: {})", 
                            event.getEventType(), correlationId, ex);
                }
                return null;
            });

        } catch (IllegalArgumentException e) {
            recordValidationErrorMetrics(event, e);
            log.error("[INV-EVENT] Validation error publishing inventory event (Correlation: {})", correlationId, e);
            throw e;
        } finally {
            timerSample.stop(Timer.builder("inventory.event.publishing.time")
                    .description("Time to publish inventory events")
                    .tag("sku", event.getSkuId())
                    .tag("location", event.getLocationId())
                    .tag("event_type", event.getEventType())
                    .register(meterRegistry));
        }
    }

    /**
     * Publishes an inventory event asynchronously.
     * Used when immediate consistency is not required.
     * 
     * @param event The inventory event to publish
     */
    public void publishInventoryEventAsync(SharedInventoryChangedEvent event) {
        try {
            publishInventoryEvent(event);
        } catch (Exception e) {
            // Log but don't propagate exception for async calls
            log.warn("[INV-EVENT] Async inventory event publishing failed for SKU: {}, Location: {}, Event: {}", 
                    event.getSkuId(), event.getLocationId(), event.getEventType(), e);
            
            // Record async failure
            meterRegistry.counter("inventory.events.async.failures",
                    "sku", event.getSkuId(),
                    "location", event.getLocationId(),
                    "event_type", event.getEventType()).increment();
        }
    }

    private void validateEvent(SharedInventoryChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Inventory changed event cannot be null");
        }
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (event.getSkuId() == null || event.getSkuId().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
        if (event.getLocationId() == null || event.getLocationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }
    }

    private void recordSuccessMetrics(SharedInventoryChangedEvent event) {
        meterRegistry.counter("inventory.events.sent",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "event_type", event.getEventType(),
                "status", "success").increment();
    }

    private void recordFailureMetrics(SharedInventoryChangedEvent event, Throwable ex) {
        meterRegistry.counter("inventory.events.errors",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "event_type", event.getEventType(),
                "error_class", ex.getClass().getSimpleName()).increment();
    }

    private void recordValidationErrorMetrics(SharedInventoryChangedEvent event, Throwable ex) {
        meterRegistry.counter("inventory.events.validation.errors",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "event_type", event.getEventType(),
                "error_class", ex.getClass().getSimpleName()).increment();
    }
}