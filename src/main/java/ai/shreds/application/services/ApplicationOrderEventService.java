package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationOrderEventInputPort;
import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.application.exceptions.ApplicationTransactionalException;
import ai.shreds.shared.dtos.SharedOrderEventMessage;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.dtos.SharedNotificationDTO;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.application.services.ApplicationCancellationService;
import ai.shreds.application.services.ApplicationReturnService;
import ai.shreds.domain.services.DomainOrderEventService;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.application.ports.ApplicationNotificationOutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling incoming order events and coordinating cancellation/return flows.
 * Manages event persistence, cancellation processing, and return eligibility updates.
 */
@Slf4j
@Service
@Transactional
public class ApplicationOrderEventService implements ApplicationOrderEventInputPort {

    private final DomainOrderEventService domainOrderEventService;
    private final ApplicationCancellationService cancellationService;
    private final ApplicationReturnService returnService;
    private final ApplicationEventOutputPort eventPublisher;
    private final ApplicationNotificationOutputPort notificationService;
    private final DomainOutputPortOrderRepository orderRepository;

    public ApplicationOrderEventService(
            DomainOrderEventService domainOrderEventService,
            ApplicationCancellationService cancellationService,
            ApplicationReturnService returnService,
            ApplicationEventOutputPort eventPublisher,
            ApplicationNotificationOutputPort notificationService,
            DomainOutputPortOrderRepository orderRepository) {
        this.domainOrderEventService = domainOrderEventService;
        this.cancellationService = cancellationService;
        this.returnService = returnService;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    @Override
    public void handleOrderEvent(SharedOrderEventMessage message) {
        try {
            log.info("Processing order event: type={}, orderId={}, oldStatus={}, newStatus={}", 
                message.getEventType(), message.getOrderId(), message.getOldStatus(), message.getNewStatus());
            
            // 1. Persist the event in domain event store
            domainOrderEventService.recordEvent(
                    message.getOrderId(),
                    message.getEventType(),
                    message.getPayload()
            );
            
            // 2. Process order status changes
            if (!message.getOldStatus().equals(message.getNewStatus())) {
                domainOrderEventService.processOrderStatusChange(
                    message.getOrderId(),
                    message.getOldStatus(),
                    message.getNewStatus()
                );
            }
            
            // 3. Trigger cancellation logic based on event type/state
            processCancellationTrigger(message);
            
            // 4. Trigger return eligibility updates
            processReturnEligibilityUpdate(message);
            
            log.debug("Successfully processed order event: {}", message.getEventId());
            
        } catch (Exception ex) {
            log.error("Failed to process order event: {} for order: {}", 
                message.getEventType(), message.getOrderId(), ex);
                
            throw new ApplicationTransactionalException(
                "Failed to process order event: " + ex.getMessage(),
                message.getOrderId(),
                "handleOrderEvent"
            );
        }
    }

    private void processCancellationTrigger(SharedOrderEventMessage message) {
        try {
            // Process shipment related cancellation decisions
            if ("SHIPPED".equalsIgnoreCase(message.getNewStatus())) {
                List<SharedCancellationResponseDTO> pending = cancellationService.getCancellationsByOrder(message.getOrderId());
                
                for (SharedCancellationResponseDTO cancellation : pending) {
                    if (SharedCancellationStatusEnum.PENDING.name().equalsIgnoreCase(cancellation.getStatus()) ||
                        SharedCancellationStatusEnum.APPROVED.name().equalsIgnoreCase(cancellation.getStatus())) {
                        
                        log.info("Auto-completing cancellation {} due to order shipment", cancellation.getCancellationId());
                        cancellationService.completeCancellation(cancellation.getCancellationId());
                    }
                }
            }
            
            // Handle payment failure events
            if ("PAYMENT_FAILED".equalsIgnoreCase(message.getEventType())) {
                log.info("Payment failed for order: {}, initiating system cancellation", message.getOrderId());
                // This would typically be handled through system cancellation message
                // but can be directly triggered here as well
            }
            
            // Handle inventory failure events
            if ("INVENTORY_ALLOCATION_FAILED".equalsIgnoreCase(message.getEventType())) {
                log.info("Inventory allocation failed for order: {}, may require cancellation", message.getOrderId());
                // This would typically be handled through system cancellation message
            }
        } catch (Exception ex) {
            log.error("Error in cancellation trigger processing: {}", ex.getMessage());
            // Don't re-throw - we want to continue with other processing
        }
    }

    private void processReturnEligibilityUpdate(SharedOrderEventMessage message) {
        try {
            // When an order is delivered, it becomes eligible for return
            if ("DELIVERED".equalsIgnoreCase(message.getNewStatus())) {
                log.info("Order {} marked as delivered, opening return eligibility window", message.getOrderId());
                
                // Publish event to notify return flows are now open
                SharedDomainEventDTO event = new SharedDomainEventDTO();
                event.setEventId(UUID.randomUUID().toString());
                event.setAggregateId(message.getOrderId());
                event.setEventType("ReturnEligibilityOpened");
                event.setEventData(Map.of(
                    "orderId", message.getOrderId(), 
                    "deliveryDate", LocalDateTime.now().toString(),
                    "originalPayload", message.getPayload()
                ));
                event.setTimestamp(LocalDateTime.now());
                event.setVersion(1);
                event.setSource("ApplicationOrderEventService");
                event.setCorrelationId(message.getEventId());
                
                eventPublisher.publishToKafka(event);
                
                // Optionally send a notification about return eligibility
                sendReturnEligibilityNotification(message.getOrderId());
            }
            
            // Handle return window closing based on business rules
            if ("RETURN_WINDOW_CLOSING".equalsIgnoreCase(message.getEventType())) {
                log.info("Return window closing soon for order: {}", message.getOrderId());
                // Could send reminder notifications to customer
            }
        } catch (Exception ex) {
            log.error("Error in return eligibility processing: {}", ex.getMessage());
            // Don't re-throw - we want to continue with other processing
        }
    }
    
    private void sendReturnEligibilityNotification(String orderId) {
        try {
            // Get customer from order snapshot
            SharedOrderSnapshotDTO orderSnapshot = orderRepository.findOrderSnapshot(orderId);
            String customerId = orderSnapshot != null ? orderSnapshot.getCustomerId() : null;
            
            if (customerId != null) {
                SharedNotificationDTO notification = new SharedNotificationDTO();
                notification.setNotificationId(UUID.randomUUID().toString());
                notification.setRecipientId(customerId);
                notification.setType("RETURN_ELIGIBILITY");
                notification.setSubject("Your order has been delivered - Return options available");
                notification.setBody(String.format(
                    "Your order %s has been delivered. If you need to return any items, you can do so within the next 30 days.", 
                    orderId
                ));
                notification.setPriority("NORMAL");
                notification.setMetadata(Map.of("orderId", orderId));
                notification.setScheduledAt(LocalDateTime.now());
                
                notificationService.sendNotification(notification);
            }
        } catch (Exception ex) {
            log.warn("Failed to send return eligibility notification: {}", ex.getMessage());
            // Non-critical - can continue without notification
        }
    }
}