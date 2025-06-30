package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationERPSyncInputPort;
import ai.shreds.shared.dtos.SharedERPUpdateMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * JMS Listener for processing ERP stock update messages.
 * Receives batch updates from ERP systems and processes them through the application layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterERPUpdateJMSListener {

    private final ApplicationERPSyncInputPort applicationERPSyncPort;
    private final MeterRegistry meterRegistry;

    /**
     * Processes incoming ERP update messages from the JMS queue.
     * Handles batch processing with proper error handling and metrics collection.
     *
     * @param message The ERP update message containing batch ID and adjustments
     * @param jmsMessage The underlying JMS message for header access
     * @param jmsHeaders Map of JMS headers
     */
    @JmsListener(
        destination = "inventory.tracking.erp.updates", 
        containerFactory = "jmsListenerContainerFactory"
    )
    @Transactional
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void onMessage(
            SharedERPUpdateMessage message,
            Message jmsMessage,
            @Header Map<String, Object> jmsHeaders) throws JMSException {
        
        Instant startTime = Instant.now();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        
        String batchId = message != null ? message.getErpBatchId() : "unknown";
        String messageId = jmsMessage.getJMSMessageID();
        String correlationId = jmsMessage.getJMSCorrelationID();
        
        try {
            // Enhanced logging with message metadata
            log.info("[ERP-SYNC] Received ERP update batch: {} (MessageID: {}, CorrelationID: {})", 
                    batchId, messageId, correlationId);
            
            // Validate message structure
            validateMessage(message);
            
            // Track message reception metrics
            meterRegistry.counter("erp.sync.messages.received",
                    "batch_id", batchId,
                    "source", getMessageSource(jmsHeaders)).increment();
            
            // Track batch size for analytics
            int adjustmentCount = message.getAdjustments() != null ? message.getAdjustments().size() : 0;
            meterRegistry.gauge("erp.sync.batch.size", adjustmentCount);
            
            log.debug("[ERP-SYNC] Processing {} adjustments in batch: {}", adjustmentCount, batchId);
            
            // Process the ERP batch through application layer
            applicationERPSyncPort.processERPBatch(message);
            
            // Calculate processing duration
            Duration processingTime = Duration.between(startTime, Instant.now());
            
            // Track successful processing metrics
            meterRegistry.counter("erp.sync.messages.processed", 
                    "batch_id", batchId,
                    "status", "success").increment();
            
            log.info("[ERP-SYNC] Successfully processed ERP batch: {} ({} adjustments in {}ms)", 
                    batchId, adjustmentCount, processingTime.toMillis());
                    
        } catch (IllegalArgumentException e) {
            // Handle validation errors specifically
            meterRegistry.counter("erp.sync.messages.errors",
                    "batch_id", batchId,
                    "error_type", "validation",
                    "error_class", e.getClass().getSimpleName()).increment();
            
            log.error("[ERP-SYNC] Validation error processing ERP batch: {} (MessageID: {})", 
                    batchId, messageId, e);
            
            // Don't retry validation errors - send to DLQ
            throw new RuntimeException("Validation failed for ERP batch: " + batchId, e);
            
        } catch (Exception e) {
            // Handle processing errors
            meterRegistry.counter("erp.sync.messages.errors",
                    "batch_id", batchId,
                    "error_type", "processing",
                    "error_class", e.getClass().getSimpleName()).increment();
            
            log.error("[ERP-SYNC] Error processing ERP batch: {} (MessageID: {})", 
                    batchId, messageId, e);
            
            // Re-throw to trigger container retry policy
            throw new RuntimeException("Failed to process ERP batch: " + batchId, e);
            
        } finally {
            // Record processing time regardless of success/failure
            timerSample.stop(Timer.builder("erp.sync.processing.time")
                    .description("Time to process ERP sync messages")
                    .tag("batch_id", batchId)
                    .register(meterRegistry));
        }
    }
    
    /**
     * Validates the incoming ERP message structure.
     *
     * @param message The ERP update message to validate
     * @throws IllegalArgumentException if the message is invalid
     */
    private void validateMessage(SharedERPUpdateMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("ERP update message cannot be null");
        }
        
        if (message.getErpBatchId() == null || message.getErpBatchId().trim().isEmpty()) {
            throw new IllegalArgumentException("ERP batch ID cannot be null or empty");
        }
        
        if (message.getAdjustments() == null) {
            throw new IllegalArgumentException("Adjustments list cannot be null");
        }
        
        if (message.getAdjustments().isEmpty()) {
            log.warn("[ERP-SYNC] Received empty adjustments list for batch: {}", message.getErpBatchId());
        }
    }
    
    /**
     * Extracts the message source from JMS headers for tracking purposes.
     *
     * @param headers JMS headers map
     * @return The source system identifier
     */
    private String getMessageSource(Map<String, Object> headers) {
        if (headers == null) {
            return "unknown";
        }
        
        String xOrigin = (String) headers.get("X-Origin");
        if (xOrigin != null && !xOrigin.trim().isEmpty()) {
            return xOrigin;
        }
        
        return "erp-system";
    }
}