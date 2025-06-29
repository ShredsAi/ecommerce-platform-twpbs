package ai.shreds.adapter.primary;

import ai.shreds.shared.dtos.SharedInventoryChangeMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * JMS Producer for publishing inventory change notifications.
 * Sends notifications about inventory level changes to downstream systems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterInventoryChangeJMSProducer {

    @Value("${app.jms.inventory.changes.destination:inventory.changes.notification}")
    private String destination;
    
    @Value("${app.jms.inventory.changes.ttl:3600000}") // 1 hour TTL
    private long messageTtl;
    
    private final JmsTemplate jmsTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Sends inventory change notification to JMS queue.
     * Uses transactional messaging to ensure consistency with database operations.
     * 
     * @param message The inventory change message to send
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public void sendInventoryChange(SharedInventoryChangeMessage message) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        String correlationId = UUID.randomUUID().toString();
        
        try {
            log.debug("[INV-CHANGE] Sending inventory change notification for SKU: {}, Location: {} (Correlation: {})", 
                      message.getSkuId(), message.getLocationId(), correlationId);
            
            // Validate message before sending
            validateMessage(message);
            
            // Send message with metadata headers
            jmsTemplate.convertAndSend(destination, message, jmsMessage -> {
                // Set message properties for routing and tracking
                jmsMessage.setStringProperty("X-Source", "INVENTORY");
                jmsMessage.setStringProperty("X-Correlation-ID", correlationId);
                jmsMessage.setStringProperty("X-SKU-ID", message.getSkuId());
                jmsMessage.setStringProperty("X-Location-ID", message.getLocationId());
                jmsMessage.setLongProperty("X-Timestamp", Instant.now().toEpochMilli());
                
                // Set message TTL to prevent old messages from being processed
                jmsMessage.setJMSExpiration(System.currentTimeMillis() + messageTtl);
                
                return jmsMessage;
            });
            
            // Record success metrics
            meterRegistry.counter("inventory.change.notifications.sent",
                    "sku", message.getSkuId(),
                    "location", message.getLocationId(),
                    "status", "success").increment();
            
            log.debug("[INV-CHANGE] Successfully sent inventory change notification (Correlation: {})", correlationId);
            
        } catch (IllegalArgumentException e) {
            // Record validation error metrics
            meterRegistry.counter("inventory.change.notifications.errors",
                    "sku", message.getSkuId(),
                    "location", message.getLocationId(),
                    "error_type", "validation",
                    "error_class", e.getClass().getSimpleName()).increment();
            
            log.error("[INV-CHANGE] Validation error sending inventory change notification (Correlation: {})", 
                    correlationId, e);
            
            throw e; // Don't retry validation errors
            
        } catch (Exception e) {
            // Record messaging error metrics
            meterRegistry.counter("inventory.change.notifications.errors",
                    "sku", message.getSkuId(),
                    "location", message.getLocationId(),
                    "error_type", "messaging",
                    "error_class", e.getClass().getSimpleName()).increment();
            
            log.error("[INV-CHANGE] Failed to send inventory change notification (Correlation: {})", 
                    correlationId, e);
            
            throw new RuntimeException("Failed to send inventory change notification for SKU: " + 
                    message.getSkuId() + ", Location: " + message.getLocationId(), e);
            
        } finally {
            // Record processing time
            timerSample.stop(Timer.builder("inventory.change.notifications.time")
                    .description("Time to send inventory change notifications")
                    .tag("sku", message.getSkuId())
                    .tag("location", message.getLocationId())
                    .register(meterRegistry));
        }
    }
    
    /**
     * Sends inventory change notification asynchronously.
     * Used when immediate consistency is not required.
     * 
     * @param message The inventory change message to send
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendInventoryChangeAsync(SharedInventoryChangeMessage message) {
        try {
            sendInventoryChange(message);
        } catch (Exception e) {
            // Log but don't propagate exception for async calls
            log.warn("[INV-CHANGE] Async inventory change notification failed for SKU: {}, Location: {}", 
                    message.getSkuId(), message.getLocationId(), e);
            
            // Record async failure
            meterRegistry.counter("inventory.change.notifications.async.failures",
                    "sku", message.getSkuId(),
                    "location", message.getLocationId()).increment();
        }
    }
    
    /**
     * Validates the inventory change message structure.
     * 
     * @param message The message to validate
     * @throws IllegalArgumentException if the message is invalid
     */
    private void validateMessage(SharedInventoryChangeMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Inventory change message cannot be null");
        }
        
        if (message.getSkuId() == null || message.getSkuId().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
        
        if (message.getLocationId() == null || message.getLocationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }
        
        if (message.getPreviousQuantity() == null && message.getNewQuantity() == null) {
            throw new IllegalArgumentException("Both previous and new quantities cannot be null");
        }
    }
}