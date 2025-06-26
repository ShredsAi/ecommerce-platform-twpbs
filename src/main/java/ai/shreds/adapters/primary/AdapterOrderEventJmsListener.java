package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationOrderEventInputPort;
import ai.shreds.shared.dtos.SharedOrderEventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdapterOrderEventJmsListener {

    private static final Logger logger = LoggerFactory.getLogger(AdapterOrderEventJmsListener.class);
    
    private final ApplicationOrderEventInputPort orderEventService;

    @Autowired
    public AdapterOrderEventJmsListener(ApplicationOrderEventInputPort orderEventService) {
        this.orderEventService = orderEventService;
    }

    @JmsListener(destination = "order-events-queue")
    public void handleOrderEvent(SharedOrderEventMessage message) {
        try {
            logger.info("Received order event message for order: {}, eventId: {}, eventType: {}", 
                       message.getOrderId(), message.getEventId(), message.getEventType());
            
            orderEventService.handleOrderEvent(message);
            
            logger.info("Successfully processed order event message for order: {}, eventType: {}", 
                       message.getOrderId(), message.getEventType());
        } catch (Exception ex) {
            logger.error("Failed to process order event message for order: {}, eventId: {}, eventType: {}", 
                        message.getOrderId(), message.getEventId(), message.getEventType(), ex);
            throw ex;
        }
    }
}