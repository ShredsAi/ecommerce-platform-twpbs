package ai.shreds.adapters.primary;

import org.springframework.stereotype.Component;
import org.springframework.jms.annotation.JmsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.shared.dtos.SharedSystemCancellationMessage;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterSystemCancellationJmsListener {

    private final ApplicationCancellationInputPort cancellationService;

    @JmsListener(destination = "${spring.jms.listener.system-cancellation-queue:systemCancellationQueue}")
    public void handleSystemCancellation(SharedSystemCancellationMessage message) {
        
        log.info("Received system cancellation message: {} for order: {}", 
                message.messageId(), message.orderId());
        
        try {
            // Validate message
            if (message == null) {
                throw new AdapterMessageProcessingException("Received null system cancellation message");
            }
            
            if (message.orderId() == null || message.orderId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Order ID is missing in system cancellation message", 
                    message.messageId(), 
                    "SystemCancellation"
                );
            }
            
            // Process the message
            cancellationService.processSystemCancellation(message);
            
            log.info("Successfully processed system cancellation message: {} for order: {}", 
                    message.messageId(), message.orderId());
                    
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for message: {}", 
                    message != null ? message.messageId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing system cancellation message: {}", 
                    message != null ? message.messageId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process system cancellation message", 
                message != null ? message.messageId() : null, 
                "SystemCancellation", 
                "systemCancellationQueue", 
                ex
            );
        }
    }
}
