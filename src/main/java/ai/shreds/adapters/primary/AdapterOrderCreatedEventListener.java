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
                event.getEventId(), event.getOrderId());
        
        try {
            // Validate event
            if (event == null) {
                throw new AdapterMessageProcessingException("Received null order created event");
            }
            
            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in order created event", 
                    event.getEventId(), 
                    "OrderCreated"
                );
            }
            
            // Only process order creation events
            if (!"ORDER_CREATED".equals(event.getEventType()) && 
                !"CREATED".equals(event.getNewStatus()) && 
                !"CONFIRMED".equals(event.getNewStatus())) {
                log.debug("Ignoring non-creation event: {} with type: {} and status: {}", 
                    event.getEventId(), event.getEventType(), event.getNewStatus());
                return;
            }
            
            // Process the order created event
            // This might involve:
            // 1. Recording the order creation in our audit log
            // 2. Setting up initial cancellation/return eligibility rules
            // 3. Preparing for potential future cancellation requests
            
            log.info("Processing order creation for order: {} with status: {}", 
                    event.getOrderId(), event.getNewStatus());
            
            orderEventService.handleOrderEvent(event);
            
            // Optional: Log order creation metadata for analytics
            if (event.getPayload() != null) {
                String customerType = (String) event.getPayload().get("customerType");
                String totalAmount = (String) event.getPayload().get("totalAmount");
                log.debug("Order {} created - Customer type: {}, Amount: {}", 
                        event.getOrderId(), customerType, totalAmount);
            }
            
            log.info("Successfully processed order created event: {} for order: {}", 
                    event.getEventId(), event.getOrderId());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for order created event: {}", 
                    event != null ? event.getEventId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing order created event: {}", 
                    event != null ? event.getEventId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process order created event", 
                event != null ? event.getEventId() : null, 
                "OrderCreated", 
                "SpringApplicationEvent", 
                ex
            );
        }
    }
}