package ai.shreds.adapters.primary;

import org.springframework.stereotype.Component;
import org.springframework.jms.annotation.JmsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationOrderEventInputPort;
import ai.shreds.shared.dtos.SharedOrderEventMessage;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterOrderEventJmsListener {

    private final ApplicationOrderEventInputPort orderEventService;

    @JmsListener(destination = "${spring.jms.listener.order-event-queue:orderEventQueue}")
    public void handleOrderEvent(SharedOrderEventMessage message) {
        
        log.info("Received order event message: {} for order: {}, event type: {}", 
                message.eventId(), message.orderId(), message.eventType());
        
        try {
            // Validate message
            if (message == null) {
                throw new AdapterMessageProcessingException("Received null order event message");
            }
            
            if (message.orderId() == null || message.orderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in order event message", 
                    message.eventId(), 
                    "OrderEvent"
                );
            }
            
            if (message.eventType() == null || message.eventType().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Event type is missing in order event message", 
                    message.eventId(), 
                    "OrderEvent"
                );
            }
            
            // Process the message
            orderEventService.handleOrderEvent(message);
            
            log.info("Successfully processed order event message: {} for order: {}, event type: {}", 
                    message.eventId(), message.orderId(), message.eventType());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for order event message: {}", 
                    message != null ? message.eventId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing order event message: {}", 
                    message != null ? message.eventId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process order event message", 
                message != null ? message.eventId() : null, 
                "OrderEvent", 
                "orderEventQueue", 
                ex
            );
        }
    }
}
