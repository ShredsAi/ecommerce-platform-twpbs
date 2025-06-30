package ai.shreds.infrastructure.external_services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationInventoryEventOutputPort;
import ai.shreds.application.ports.ApplicationLowStockAlertOutputPort;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import ai.shreds.shared.dtos.SharedReservationConfirmedEventDTO;
import ai.shreds.shared.dtos.SharedReservationCreatedEventDTO;
import ai.shreds.shared.dtos.SharedReservationExpiredEventDTO;
import ai.shreds.shared.dtos.SharedReservationFailedEventDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionExternalServiceError;

import java.util.concurrent.CompletableFuture;

@Service
public class InfrastructureKafkaEventPublisher implements ApplicationInventoryEventOutputPort, 
        ApplicationLowStockAlertOutputPort, ApplicationEventPublisherOutputPort {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String inventoryEventTopic;
    private final String lowStockAlertTopic;
    private final String reservationCreatedTopic;
    private final String reservationFailedTopic;
    private final String reservationConfirmedTopic;
    private final String reservationExpiredTopic;

    public InfrastructureKafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.inventory-events:inventory-events}") String inventoryEventTopic,
            @Value("${kafka.topics.low-stock-alerts:low-stock-alerts}") String lowStockAlertTopic,
            @Value("${kafka.topics.reservation-created:reservation-created}") String reservationCreatedTopic,
            @Value("${kafka.topics.reservation-failed:reservation-failed}") String reservationFailedTopic,
            @Value("${kafka.topics.reservation-confirmed:reservation-confirmed}") String reservationConfirmedTopic,
            @Value("${kafka.topics.reservation-expired:reservation-expired}") String reservationExpiredTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.inventoryEventTopic = inventoryEventTopic;
        this.lowStockAlertTopic = lowStockAlertTopic;
        this.reservationCreatedTopic = reservationCreatedTopic;
        this.reservationFailedTopic = reservationFailedTopic;
        this.reservationConfirmedTopic = reservationConfirmedTopic;
        this.reservationExpiredTopic = reservationExpiredTopic;
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

    @Override
    public void publishReservationCreated(SharedReservationCreatedEventDTO event) {
        try {
            log.debug("Publishing reservation created event for reservationId: {}", event.getReservationId());

            String key = event.getReservationId();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(reservationCreatedTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish reservation created event for reservationId: {}",
                            event.getReservationId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish reservation created event: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published reservation created event for reservationId: {} to topic: {} with offset: {}",
                            event.getReservationId(), reservationCreatedTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing reservation created event for reservationId: {}",
                    event.getReservationId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish reservation created event: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishReservationFailed(SharedReservationFailedEventDTO event) {
        try {
            log.debug("Publishing reservation failed event for cartId: {}", event.getCartId());

            String key = event.getCartId();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(reservationFailedTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish reservation failed event for cartId: {}",
                            event.getCartId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish reservation failed event: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published reservation failed event for cartId: {} to topic: {} with offset: {}",
                            event.getCartId(), reservationFailedTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing reservation failed event for cartId: {}",
                    event.getCartId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish reservation failed event: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishReservationConfirmed(SharedReservationConfirmedEventDTO event) {
        try {
            log.debug("Publishing reservation confirmed event for reservationId: {}", event.getReservationId());

            String key = event.getReservationId();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(reservationConfirmedTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish reservation confirmed event for reservationId: {}",
                            event.getReservationId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish reservation confirmed event: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published reservation confirmed event for reservationId: {} to topic: {} with offset: {}",
                            event.getReservationId(), reservationConfirmedTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing reservation confirmed event for reservationId: {}",
                    event.getReservationId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish reservation confirmed event: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishReservationExpired(SharedReservationExpiredEventDTO event) {
        try {
            log.debug("Publishing reservation expired event for reservationId: {}", event.getReservationId());

            String key = event.getReservationId();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(reservationExpiredTopic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish reservation expired event for reservationId: {}",
                            event.getReservationId(), ex);
                    throw new InfrastructureExceptionExternalServiceError("Kafka",
                            "Failed to publish reservation expired event: " + ex.getMessage(), ex);
                } else {
                    log.debug("Successfully published reservation expired event for reservationId: {} to topic: {} with offset: {}",
                            event.getReservationId(), reservationExpiredTopic,
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing reservation expired event for reservationId: {}",
                    event.getReservationId(), e);
            throw new InfrastructureExceptionExternalServiceError("Kafka",
                    "Failed to publish reservation expired event: " + e.getMessage(), e);
        }
    }
}
