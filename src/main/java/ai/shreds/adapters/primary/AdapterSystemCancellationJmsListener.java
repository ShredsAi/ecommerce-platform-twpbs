package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.shared.dtos.SharedSystemCancellationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdapterSystemCancellationJmsListener {

    private static final Logger logger = LoggerFactory.getLogger(AdapterSystemCancellationJmsListener.class);
    
    private final ApplicationCancellationInputPort cancellationService;

    @Autowired
    public AdapterSystemCancellationJmsListener(ApplicationCancellationInputPort cancellationService) {
        this.cancellationService = cancellationService;
    }

    @JmsListener(destination = "system-cancellation-queue")
    public void handleSystemCancellation(SharedSystemCancellationMessage message) {
        try {
            logger.info("Received system cancellation message for order: {}, messageId: {}", 
                       message.getOrderId(), message.getMessageId());
            
            cancellationService.processSystemCancellation(message);
            
            logger.info("Successfully processed system cancellation message for order: {}", 
                       message.getOrderId());
        } catch (Exception ex) {
            logger.error("Failed to process system cancellation message for order: {}, messageId: {}", 
                        message.getOrderId(), message.getMessageId(), ex);
            throw ex;
        }
    }
}