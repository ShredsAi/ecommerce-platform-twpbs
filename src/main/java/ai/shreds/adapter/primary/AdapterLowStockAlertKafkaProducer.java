package ai.shreds.adapter.primary;

import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
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
 * Kafka Producer for publishing low stock alerts.
 * Sends alerts about low inventory levels to downstream systems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterLowStockAlertKafkaProducer {

    @Value("${app.kafka.alerts.lowStock.topic:inventory.alerts.low-stock}")
    private String topic;
    
    private final KafkaTemplate<String, SharedLowStockAlertEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Publishes a low stock alert event to Kafka.
     * Uses retryable mechanism to handle transient errors.
     * 
     * @param event The low stock alert event to publish
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public void publishLowStockAlert(SharedLowStockAlertEvent event) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        String key = event.getSkuId() + "|" + event.getLocationId();
        String correlationId = UUID.randomUUID().toString();

        try {
            log.debug("[LOW-STOCK-ALERT] Publishing low stock alert for SKU: {}, Location: {} (Correlation: {})", 
                      event.getSkuId(), event.getLocationId(), correlationId);

            validateEvent(event);

            CompletableFuture<SendResult<String, SharedLowStockAlertEvent>> future =
                    kafkaTemplate.send(topic, key, event);
            
            future.handle((result, ex) -> {
                if (result != null) {
                    recordSuccessMetrics(event);
                    log.debug("[LOW-STOCK-ALERT] Successfully published low stock alert (Correlation: {})", correlationId);
                } else {
                    recordFailureMetrics(event, ex);
                    log.error("[LOW-STOCK-ALERT] Failed to publish low stock alert (Correlation: {})", correlationId, ex);
                }
                return null;
            });

        } catch (IllegalArgumentException e) {
            recordValidationErrorMetrics(event, e);
            log.error("[LOW-STOCK-ALERT] Validation error publishing low stock alert (Correlation: {})", correlationId, e);
            throw e;
        } finally {
            timerSample.stop(Timer.builder("low.stock.alert.publishing.time")
                    .description("Time to publish low stock alerts")
                    .tag("sku", event.getSkuId())
                    .tag("location", event.getLocationId())
                    .register(meterRegistry));
        }
    }

    private void validateEvent(SharedLowStockAlertEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Low stock alert event cannot be null");
        }
        if (event.getSkuId() == null || event.getSkuId().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
        if (event.getLocationId() == null || event.getLocationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }
    }

    private void recordSuccessMetrics(SharedLowStockAlertEvent event) {
        meterRegistry.counter("low.stock.alerts.sent",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "status", "success").increment();
    }

    private void recordFailureMetrics(SharedLowStockAlertEvent event, Throwable ex) {
        meterRegistry.counter("low.stock.alerts.errors",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "error_class", ex.getClass().getSimpleName()).increment();
    }

    private void recordValidationErrorMetrics(SharedLowStockAlertEvent event, Throwable ex) {
        meterRegistry.counter("low.stock.alerts.validation.errors",
                "sku", event.getSkuId(),
                "location", event.getLocationId(),
                "error_class", ex.getClass().getSimpleName()).increment();
    }
}