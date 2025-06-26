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
                event.getEventId(), event.getOrderId());
        
        try {
            // Validate event
            if (event == null) {
                throw new AdapterMessageProcessingException("Received null order cancelled event");
            }
            
            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in order cancelled event", 
                    event.getEventId(), 
                    "OrderCancelled"
                );
            }
            
            if (!"ORDER_CANCELLED".equals(event.getEventType()) && !"CANCELLED".equals(event.getNewStatus())) {
                log.debug("Ignoring non-cancellation event: {} with type: {} and status: {}", 
                    event.getEventId(), event.getEventType(), event.getNewStatus());
                return;
            }
            
            // Check if this is an external cancellation that needs to be recorded
            if (event.getPayload() != null && event.getPayload().containsKey("source")) {
                String source = (String) event.getPayload().get("source");
                if ("EXTERNAL_SYSTEM".equals(source) || "FRAUD_DETECTION".equals(source)) {
                    log.info("Processing external cancellation from source: {} for order: {}", 
                            source, event.getOrderId());
                    
                    // Create system cancellation message
                    var systemMessage = new ai.shreds.shared.dtos.SharedSystemCancellationMessage(
                        event.getEventId(),
                        event.getOrderId(),
                        (String) event.getPayload().getOrDefault("reason", "SYSTEM_CANCELLATION"),
                        source,
                        event.getTimestamp(),
                        event.getPayload()
                    );
                    
                    cancellationService.processSystemCancellation(systemMessage);
                }
            }
            
            log.info("Successfully processed order cancelled event: {} for order: {}", 
                    event.getEventId(), event.getOrderId());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for order cancelled event: {}", 
                    event != null ? event.getEventId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing order cancelled event: {}", 
                    event != null ? event.getEventId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process order cancelled event", 
                event != null ? event.getEventId() : null, 
                "OrderCancelled", 
                "SpringApplicationEvent", 
                ex
            );
        }
    }
}