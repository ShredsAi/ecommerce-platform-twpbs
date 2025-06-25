package ai.shreds.adapters.primary;

import ai.shreds.adapters.exceptions.AdapterEventPublishingException;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Adapter for publishing Spring Application Events.
 * Ensures events are published after transaction commit to maintain consistency.
 */
@Component
@Slf4j
public class AdapterSpringEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public AdapterSpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    /**
     * Publishes OrderCreated event after transaction commit.
     * This ensures the event is only published if the database transaction succeeds.
     */
    public void publishOrderCreated(SharedOrderCreatedEventDTO event) {
        try {
            addEventHeaders(event, event.getCorrelationId());
            
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                // Publish after commit for production
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.debug("Publishing OrderCreated event after commit for orderId: {}", event.getOrderId());
                        applicationEventPublisher.publishEvent(event);
                    }
                });
            } else {
                // Immediate publish when not in transaction (e.g., tests)
                log.debug("Publishing OrderCreated event immediately for orderId: {}", event.getOrderId());
                applicationEventPublisher.publishEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish OrderCreated event", e);
            throw new AdapterEventPublishingException(
                "Failed to publish OrderCreated event", 
                e, 
                "OrderCreated", 
                event
            );
        }
    }
    
    /**
     * Publishes OrderCreationFailed event after transaction commit or rollback.
     */
    public void publishOrderCreationFailed(SharedOrderCreationFailedEventDTO event) {
        try {
            addEventHeaders(event, event.getCorrelationId());
            
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                // Publish after completion (commit or rollback) for failure events
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        log.debug("Publishing OrderCreationFailed event after completion for cartId: {}", event.getCartId());
                        applicationEventPublisher.publishEvent(event);
                    }
                });
            } else {
                // Immediate publish when not in transaction
                log.debug("Publishing OrderCreationFailed event immediately for cartId: {}", event.getCartId());
                applicationEventPublisher.publishEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish OrderCreationFailed event", e);
            throw new AdapterEventPublishingException(
                "Failed to publish OrderCreationFailed event", 
                e, 
                "OrderCreationFailed", 
                event
            );
        }
    }
    
    /**
     * Adds standard event headers for tracing and monitoring.
     */
    private void addEventHeaders(Object event, String correlationId) {
        // In a real scenario, you might want to add headers to the event
        // For now, we'll just ensure the event has required fields
        if (event instanceof SharedOrderCreatedEventDTO) {
            SharedOrderCreatedEventDTO orderEvent = (SharedOrderCreatedEventDTO) event;
            if (orderEvent.getEventId() == null) {
                orderEvent.setEventId(UUID.randomUUID().toString());
            }
            if (orderEvent.getOccurredOn() == null) {
                orderEvent.setOccurredOn(java.time.Instant.now());
            }
            if (orderEvent.getCorrelationId() == null && correlationId != null) {
                orderEvent.setCorrelationId(correlationId);
            }
        } else if (event instanceof SharedOrderCreationFailedEventDTO) {
            SharedOrderCreationFailedEventDTO failedEvent = (SharedOrderCreationFailedEventDTO) event;
            if (failedEvent.getEventId() == null) {
                failedEvent.setEventId(UUID.randomUUID().toString());
            }
            if (failedEvent.getOccurredOn() == null) {
                failedEvent.setOccurredOn(java.time.Instant.now());
            }
            if (failedEvent.getCorrelationId() == null && correlationId != null) {
                failedEvent.setCorrelationId(correlationId);
            }
        }
    }
}