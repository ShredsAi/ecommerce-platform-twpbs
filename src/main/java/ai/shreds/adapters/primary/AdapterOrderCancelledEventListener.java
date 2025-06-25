package ai.shreds.adapters.primary;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.shared.dtos.SharedOrderEventMessage;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterOrderCancelledEventListener {

    private final ApplicationCancellationInputPort cancellationService;

    @EventListener
    public void handleOrderCancelledEvent(SharedOrderEventMessage event) {
        
        log.info("Received order cancelled event: {} for order: {}", 
                event.eventId(), event.orderId());
        
        try {
            // Validate event
            if (event == null) {
                throw new AdapterMessageProcessingException("Received null order cancelled event");
            }
            
            if (event.orderId() == null || event.orderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in order cancelled event", 
                    event.eventId(), 
                    "OrderCancelled"
                );
            }
            
            if (!"ORDER_CANCELLED".equals(event.eventType()) && !"CANCELLED".equals(event.newStatus())) {
                log.debug("Ignoring non-cancellation event: {} with type: {} and status: {}", 
                    event.eventId(), event.eventType(), event.newStatus());
                return;
            }
            
            // Check if this is an external cancellation that needs to be recorded
            if (event.payload() != null && event.payload().containsKey("source")) {
                String source = (String) event.payload().get("source");
                if ("EXTERNAL_SYSTEM".equals(source) || "FRAUD_DETECTION".equals(source)) {
                    log.info("Processing external cancellation from source: {} for order: {}", 
                            source, event.orderId());
                    
                    // Create system cancellation message
                    var systemMessage = new ai.shreds.shared.dtos.SharedSystemCancellationMessage(
                        event.eventId(),
                        event.orderId(),
                        (String) event.payload().getOrDefault("reason", "SYSTEM_CANCELLATION"),
                        source,
                        event.timestamp(),
                        event.payload()
                    );
                    
                    cancellationService.processSystemCancellation(systemMessage);
                }
            }
            
            log.info("Successfully processed order cancelled event: {} for order: {}", 
                    event.eventId(), event.orderId());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for order cancelled event: {}", 
                    event != null ? event.eventId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing order cancelled event: {}", 
                    event != null ? event.eventId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process order cancelled event", 
                event != null ? event.eventId() : null, 
                "OrderCancelled", 
                "SpringApplicationEvent", 
                ex
            );
        }
    }
}
