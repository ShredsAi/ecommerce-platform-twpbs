package ai.shreds.adapters.primary;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationOrderEventInputPort;
import ai.shreds.shared.dtos.SharedOrderEventMessage;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterOrderCreatedEventListener {

    private final ApplicationOrderEventInputPort orderEventService;

    @EventListener
    public void handleOrderCreatedEvent(SharedOrderEventMessage event) {
        
        log.info("Received order created event: {} for order: {}", 
                event.eventId(), event.orderId());
        
        try {
            // Validate event
            if (event == null) {
                throw new AdapterMessageProcessingException("Received null order created event");
            }
            
            if (event.orderId() == null || event.orderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in order created event", 
                    event.eventId(), 
                    "OrderCreated"
                );
            }
            
            // Only process order creation events
            if (!"ORDER_CREATED".equals(event.eventType()) && 
                !"CREATED".equals(event.newStatus()) && 
                !"CONFIRMED".equals(event.newStatus())) {
                log.debug("Ignoring non-creation event: {} with type: {} and status: {}", 
                    event.eventId(), event.eventType(), event.newStatus());
                return;
            }
            
            // Process the order created event
            // This might involve:
            // 1. Recording the order creation in our audit log
            // 2. Setting up initial cancellation/return eligibility rules
            // 3. Preparing for potential future cancellation requests
            
            log.info("Processing order creation for order: {} with status: {}", 
                    event.orderId(), event.newStatus());
            
            orderEventService.handleOrderEvent(event);
            
            // Optional: Log order creation metadata for analytics
            if (event.payload() != null) {
                String customerType = (String) event.payload().get("customerType");
                String totalAmount = (String) event.payload().get("totalAmount");
                log.debug("Order {} created - Customer type: {}, Amount: {}", 
                        event.orderId(), customerType, totalAmount);
            }
            
            log.info("Successfully processed order created event: {} for order: {}", 
                    event.eventId(), event.orderId());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for order created event: {}", 
                    event != null ? event.eventId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing order created event: {}", 
                    event != null ? event.eventId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process order created event", 
                event != null ? event.eventId() : null, 
                "OrderCreated", 
                "SpringApplicationEvent", 
                ex
            );
        }
    }
}
