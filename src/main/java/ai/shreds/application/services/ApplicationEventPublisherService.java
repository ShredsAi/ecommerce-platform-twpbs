package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationEventPublisherInputPort;
import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.application.ports.ApplicationNotificationOutputPort;
import ai.shreds.application.exceptions.ApplicationTransactionalException;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.shared.dtos.SharedNotificationDTO;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * Service for publishing domain events and sending related notifications.
 * Orchestrates event publishing across multiple channels (Kafka, JMS, Spring Events).
 */
@Slf4j
@Service
public class ApplicationEventPublisherService implements ApplicationEventPublisherInputPort {

    private final ApplicationEventOutputPort eventOutput;
    private final ApplicationNotificationOutputPort notificationService;

    public ApplicationEventPublisherService(
            ApplicationEventOutputPort eventOutput,
            ApplicationNotificationOutputPort notificationService) {
        this.eventOutput = eventOutput;
        this.notificationService = notificationService;
    }

    @Override
    public void publishCancellationEvent(Object event) {
        try {
            log.info("Publishing cancellation event: {}", event.getClass().getSimpleName());
            
            SharedDomainEventDTO domainEvent = createDomainEvent(event, "CancellationEvent");
            
            // Extract relevant data for better event structure
            if (event instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) event;
                domainEvent.setAggregateId((String) eventMap.get("cancellationId"));
                domainEvent.setCorrelationId((String) eventMap.get("orderId"));
                
                Map<String, Object> eventData = new HashMap<>(domainEvent.getEventData());
                eventData.putAll(eventMap);
                domainEvent.setEventData(eventData);
            }
            
            // Publish to all channels
            publishToAllChannels(domainEvent, event);
            
            // Send related notification
            sendCancellationNotification(domainEvent);
            
            log.debug("Successfully published cancellation event: {}", domainEvent.getEventId());
            
        } catch (Exception ex) {
            log.error("Failed to publish cancellation event", ex);
            throw new ApplicationTransactionalException(
                "Failed to publish cancellation event: " + ex.getMessage(),
                null,
                "publishCancellationEvent"
            );
        }
    }

    @Override
    public void publishReturnEvent(Object event) {
        try {
            log.info("Publishing return event: {}", event.getClass().getSimpleName());
            
            SharedDomainEventDTO domainEvent = createDomainEvent(event, "ReturnEvent");
            
            // Extract relevant data for better event structure
            if (event instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) event;
                domainEvent.setAggregateId((String) eventMap.get("returnId"));
                domainEvent.setCorrelationId((String) eventMap.get("orderId"));
                
                Map<String, Object> eventData = new HashMap<>(domainEvent.getEventData());
                eventData.putAll(eventMap);
                domainEvent.setEventData(eventData);
            }
            
            // Publish to all channels
            publishToAllChannels(domainEvent, event);
            
            // Send related notification
            sendReturnNotification(domainEvent);
            
            log.debug("Successfully published return event: {}", domainEvent.getEventId());
            
        } catch (Exception ex) {
            log.error("Failed to publish return event", ex);
            throw new ApplicationTransactionalException(
                "Failed to publish return event: " + ex.getMessage(),
                null,
                "publishReturnEvent"
            );
        }
    }

    private void publishToAllChannels(SharedDomainEventDTO domainEvent, Object originalEvent) {
        // Publish to Kafka (primary channel)
        try {
            eventOutput.publishToKafka(domainEvent);
            log.debug("Published to Kafka: {}", domainEvent.getEventId());
        } catch (Exception ex) {
            log.error("Failed to publish to Kafka: {}", domainEvent.getEventId(), ex);
            // Continue with other channels even if Kafka fails
        }
        
        // Publish to JMS (secondary channel)
        try {
            eventOutput.publishToJms(domainEvent);
            log.debug("Published to JMS: {}", domainEvent.getEventId());
        } catch (Exception ex) {
            log.error("Failed to publish to JMS: {}", domainEvent.getEventId(), ex);
            // Continue with other channels even if JMS fails
        }
        
        // Publish to Spring Events (internal processing)
        try {
            eventOutput.publishToSpringEvents(originalEvent);
            log.debug("Published to Spring Events: {}", domainEvent.getEventId());
        } catch (Exception ex) {
            log.error("Failed to publish to Spring Events: {}", domainEvent.getEventId(), ex);
            // Continue - Spring Events failure shouldn't break external publishing
        }
    }

    private SharedDomainEventDTO createDomainEvent(Object source, String eventType) {
        SharedDomainEventDTO dto = new SharedDomainEventDTO();
        dto.setEventId(UUID.randomUUID().toString());
        
        // Try to extract aggregate ID from source if it's a Map
        if (source instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sourceMap = (Map<String, Object>) source;
            dto.setAggregateId((String) sourceMap.get("aggregateId"));
            dto.setCorrelationId((String) sourceMap.get("orderId"));
        }
        
        dto.setEventType(eventType);
        dto.setEventData(Map.of(
            "payload", source,
            "publishedBy", getClass().getSimpleName(),
            "publishedAt", LocalDateTime.now().toString()
        ));
        dto.setTimestamp(LocalDateTime.now());
        dto.setVersion(1);
        dto.setSource(getClass().getSimpleName());
        
        return dto;
    }

    private void sendCancellationNotification(SharedDomainEventDTO domainEvent) {
        try {
            SharedNotificationDTO notification = new SharedNotificationDTO();
            notification.setNotificationId(UUID.randomUUID().toString());
            
            // Extract recipient from event data if available
            Map<String, Object> eventData = domainEvent.getEventData();
            String customerId = extractCustomerId(eventData);
            
            if (customerId != null) {
                notification.setRecipientId(customerId);
            }
            
            notification.setType("CANCELLATION_EVENT_NOTIFICATION");
            notification.setSubject("Order Cancellation Update");
            notification.setBody(buildCancellationNotificationBody(domainEvent));
            notification.setPriority("HIGH");
            notification.setMetadata(Map.of(
                "eventId", domainEvent.getEventId(),
                "eventType", domainEvent.getEventType(),
                "aggregateId", domainEvent.getAggregateId() != null ? domainEvent.getAggregateId() : "N/A"
            ));
            notification.setScheduledAt(LocalDateTime.now());
            
            notificationService.sendNotification(notification);
            
        } catch (Exception ex) {
            log.warn("Failed to send cancellation notification for event: {}", domainEvent.getEventId(), ex);
            // Don't throw - notification failure shouldn't break event publishing
        }
    }

    private void sendReturnNotification(SharedDomainEventDTO domainEvent) {
        try {
            SharedNotificationDTO notification = new SharedNotificationDTO();
            notification.setNotificationId(UUID.randomUUID().toString());
            
            // Extract recipient from event data if available
            Map<String, Object> eventData = domainEvent.getEventData();
            String customerId = extractCustomerId(eventData);
            
            if (customerId != null) {
                notification.setRecipientId(customerId);
            }
            
            notification.setType("RETURN_EVENT_NOTIFICATION");
            notification.setSubject("Order Return Update");
            notification.setBody(buildReturnNotificationBody(domainEvent));
            notification.setPriority("NORMAL");
            notification.setMetadata(Map.of(
                "eventId", domainEvent.getEventId(),
                "eventType", domainEvent.getEventType(),
                "aggregateId", domainEvent.getAggregateId() != null ? domainEvent.getAggregateId() : "N/A"
            ));
            notification.setScheduledAt(LocalDateTime.now());
            
            notificationService.sendNotification(notification);
            
        } catch (Exception ex) {
            log.warn("Failed to send return notification for event: {}", domainEvent.getEventId(), ex);
            // Don't throw - notification failure shouldn't break event publishing
        }
    }

    private String extractCustomerId(Map<String, Object> eventData) {
        // Try different possible keys for customer ID
        Object payload = eventData.get("payload");
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            return (String) payloadMap.get("customerId");
        }
        
        return (String) eventData.get("customerId");
    }

    private String buildCancellationNotificationBody(SharedDomainEventDTO domainEvent) {
        return String.format(
            "A cancellation event has occurred. Event ID: %s, Type: %s. Please check your order status for more details.",
            domainEvent.getEventId(),
            domainEvent.getEventType()
        );
    }

    private String buildReturnNotificationBody(SharedDomainEventDTO domainEvent) {
        return String.format(
            "A return event has occurred. Event ID: %s, Type: %s. Please check your return status for more details.",
            domainEvent.getEventId(),
            domainEvent.getEventType()
        );
    }
}