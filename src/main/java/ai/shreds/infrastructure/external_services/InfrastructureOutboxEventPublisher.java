package ai.shreds.infrastructure.external_services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityOutboxEvent;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionExternalServiceError;
import ai.shreds.infrastructure.repositories.InfrastructureJpaOutboxEventRepository;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class InfrastructureOutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureOutboxEventPublisher.class);

    private final InfrastructureJpaOutboxEventRepository jpaRepository;
    private final InfrastructureKafkaEventPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final int maxRetries;

    public InfrastructureOutboxEventPublisher(InfrastructureJpaOutboxEventRepository jpaRepository,
                                              InfrastructureKafkaEventPublisher kafkaPublisher,
                                              ObjectMapper objectMapper,
                                              @Value("${outbox.batch-size:100}") int batchSize,
                                              @Value("${outbox.max-retries:3}") int maxRetries) {
        this.jpaRepository = jpaRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    /**
     * Process outbox events periodically based on cron expression
     */
    @Scheduled(cron = "${outbox.processing.cron:*/30 * * * * *}")
    public void scheduledProcessOutboxEvents() {
        try {
            int processed = processOutboxEvents();
            if (processed > 0) {
                log.info("Scheduled outbox processing completed: {} events processed", processed);
            }
        } catch (Exception e) {
            log.error("Scheduled outbox processing failed", e);
        }
    }

    /**
     * Process a batch of outbox events and publish them to Kafka
     * @return number of events processed
     */
    @Transactional(readOnly = true)
    public int processOutboxEvents() {
        try {
            log.debug("Fetching up to {} unprocessed outbox events", batchSize);
            List<InfrastructureJpaEntityOutboxEvent> events;
            if (batchSize > 0) {
                events = jpaRepository.findTop100ByProcessedFalseOrderByOccurredOn();
                if (events.size() > batchSize) {
                    events = events.subList(0, batchSize);
                }
            } else {
                events = jpaRepository.findTop100ByProcessedFalseOrderByOccurredOn();
            }
            
            if (events.isEmpty()) {
                log.debug("No unprocessed outbox events found");
                return 0;
            }
            
            log.info("Processing {} outbox events", events.size());
            int processedCount = 0;
            
            for (InfrastructureJpaEntityOutboxEvent event : events) {
                try {
                    publishEvent(event);
                    markAsProcessed(event.getEventId());
                    processedCount++;
                } catch (Exception e) {
                    log.error("Failed to process outbox event: {}", event.getEventId(), e);
                }
            }
            
            log.info("Successfully processed {}/{} outbox events", processedCount, events.size());
            return processedCount;
        } catch (Exception e) {
            log.error("Failed to process outbox events", e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to process outbox events: " + e.getMessage(), e);
        }
    }

    /**
     * Publish a single event to Kafka with retries
     */
    private void publishEvent(InfrastructureJpaEntityOutboxEvent event) {
        log.debug("Publishing outbox event: {}, type: {}", event.getEventId(), event.getEventType());
        
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Parse and publish event based on type
                if ("InventoryChangedEvent".equals(event.getEventType())) {
                    SharedInventoryChangedEvent inventoryEvent = objectMapper.readValue(event.getPayload(), SharedInventoryChangedEvent.class);
                    kafkaPublisher.publishInventoryChange(inventoryEvent);
                } else if ("LowStockAlertEvent".equals(event.getEventType())) {
                    SharedLowStockAlertEvent alertEvent = objectMapper.readValue(event.getPayload(), SharedLowStockAlertEvent.class);
                    kafkaPublisher.publishLowStockAlert(alertEvent);
                } else {
                    log.warn("Unknown event type in outbox: {}", event.getEventType());
                    return; // Skip unknown event types
                }
                
                log.debug("Successfully published outbox event: {}", event.getEventId());
                return; // Success
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to publish outbox event (attempt {}/{}): {}", 
                    attempt, maxRetries, event.getEventId(), e);
                
                // Simple backoff before retry
                try {
                    Thread.sleep(attempt * 100L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // If we got here, all retries failed
        throw new InfrastructureExceptionExternalServiceError("KafkaPublisher", 
            "Failed to publish outbox event after " + maxRetries + " attempts", 
            lastException);
    }

    /**
     * Mark an event as processed in a separate transaction
     */
    @Transactional
    public void markAsProcessed(UUID eventId) {
        try {
            log.debug("Marking outbox event as processed: {}", eventId);
            
            InfrastructureJpaEntityOutboxEvent event = jpaRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Outbox event not found for marking as processed: {}", eventId);
                    return new InfrastructureExceptionDatabaseError(
                        "Outbox event not found: " + eventId);
                });
                
            event.setProcessed(true);
            event.setProcessedOn(Instant.now());
            jpaRepository.save(event);
            
            log.debug("Successfully marked outbox event as processed: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to mark outbox event as processed: {}", eventId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to mark outbox event as processed: " + e.getMessage(), e);
        }
    }
}