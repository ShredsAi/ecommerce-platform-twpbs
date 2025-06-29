package ai.shreds.infrastructure.external_services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import ai.shreds.application.ports.ApplicationInventoryEventOutputPort;
import ai.shreds.application.ports.ApplicationLowStockAlertOutputPort;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionExternalServiceError;

import java.util.concurrent.CompletableFuture;

@Service
public class InfrastructureKafkaEventPublisher implements ApplicationInventoryEventOutputPort, ApplicationLowStockAlertOutputPort {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String inventoryEventTopic;
    private final String lowStockAlertTopic;

    public InfrastructureKafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.inventory-events:inventory-events}") String inventoryEventTopic,
            @Value("${kafka.topics.low-stock-alerts:low-stock-alerts}") String lowStockAlertTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.inventoryEventTopic = inventoryEventTopic;
        this.lowStockAlertTopic = lowStockAlertTopic;
    }

    @Override
    public void publishInventoryChange(SharedInventoryChangedEvent event) {
        try {
            log.debug("Publishing inventory change event for SKU: {} at location: {}",
                    event.getSkuId(), event.getLocationId());

            String key = String.format("%s:%s", event.getSkuId(), event.getLocationId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(inventoryEventTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish inventory change event for SKU: {} at location: {}",
                            event.getSkuId(), event.getLocationId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish inventory change event: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published inventory change event for SKU: {} at location: {} to topic: {} with offset: {}",
                            event.getSkuId(), event.getLocationId(), inventoryEventTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing inventory change event for SKU: {} at location: {}",
                    event.getSkuId(), event.getLocationId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish inventory change event: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishLowStockAlert(SharedLowStockAlertEvent event) {
        try {
            log.debug("Publishing low stock alert for SKU: {} at location: {}",
                    event.getSkuId(), event.getLocationId());

            String key = String.format("%s:%s", event.getSkuId(), event.getLocationId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(lowStockAlertTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish low stock alert for SKU: {} at location: {}",
                            event.getSkuId(), event.getLocationId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish low stock alert: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published low stock alert for SKU: {} at location: {} to topic: {} with offset: {}",
                            event.getSkuId(), event.getLocationId(), lowStockAlertTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing low stock alert for SKU: {} at location: {}",
                    event.getSkuId(), event.getLocationId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish low stock alert: " + e.getMessage(), e);
        }
    }
}