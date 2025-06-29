package ai.shreds.infrastructure.external_services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import ai.shreds.application.ports.ApplicationInventoryChangeNotificationOutputPort;
import ai.shreds.shared.dtos.SharedInventoryChangeMessage;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionExternalServiceError;

@Service
public class InfrastructureJMSNotificationSender implements ApplicationInventoryChangeNotificationOutputPort {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureJMSNotificationSender.class);

    private final JmsTemplate jmsTemplate;
    private final String inventoryChangeQueue;

    public InfrastructureJMSNotificationSender(
            JmsTemplate jmsTemplate,
            @Value("${spring.jms.queues.inventory-changes:inventory.changes.notification}") String inventoryChangeQueue) {
        this.jmsTemplate = jmsTemplate;
        this.inventoryChangeQueue = inventoryChangeQueue;
    }

    @Override
    public void sendInventoryChangeNotification(SharedInventoryChangeMessage message) {
        try {
            log.debug("Sending inventory change notification for SKU: {} at location: {}", 
                message.getSkuId(), message.getLocationId());
            
            jmsTemplate.convertAndSend(inventoryChangeQueue, message, messagePostProcessor -> {
                messagePostProcessor.setStringProperty("X-Source", "INVENTORY");
                messagePostProcessor.setStringProperty("X-Event-Type", "INVENTORY_CHANGE");
                messagePostProcessor.setStringProperty("X-SKU-ID", message.getSkuId());
                messagePostProcessor.setStringProperty("X-Location-ID", message.getLocationId());
                messagePostProcessor.setJMSCorrelationID(generateCorrelationId(message));
                return messagePostProcessor;
            });
            
            log.info("Successfully sent inventory change notification for SKU: {} at location: {}", 
                message.getSkuId(), message.getLocationId());
                
        } catch (Exception e) {
            log.error("Failed to send inventory change notification for SKU: {} at location: {}", 
                message.getSkuId(), message.getLocationId(), e);
            throw new InfrastructureExceptionExternalServiceError(
                "JMS", 
                "Failed to send inventory change notification: " + e.getMessage(), 
                e
            );
        }
    }

    private String generateCorrelationId(SharedInventoryChangeMessage message) {
        return String.format("%s_%s_%d", 
            message.getSkuId(), 
            message.getLocationId(), 
            System.currentTimeMillis());
    }
}
