package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationTransactionalException;
import ai.shreds.application.exceptions.ApplicationSagaException;
import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.application.ports.ApplicationInventoryServiceOutputPort;
import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.application.ports.ApplicationNotificationOutputPort;
import ai.shreds.domain.ports.DomainInputPortReturnService;
import ai.shreds.domain.ports.DomainOutputPortReturnRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.entities.DomainReturnItemEntity;
import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.value_objects.SharedReturnRequestParams;
import ai.shreds.shared.value_objects.SharedReturnItemParams;
import ai.shreds.shared.dtos.SharedReturnResponseDTO;
import ai.shreds.shared.dtos.SharedReturnRequestDTO;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.shared.dtos.SharedNotificationDTO;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.enums.SharedReturnReasonEnum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for handling return use-cases.
 * Orchestrates domain logic with external services for return processing.
 */
@Slf4j
@Service
@Transactional
public class ApplicationReturnService implements ApplicationReturnInputPort {

    private final DomainInputPortReturnService domainReturnService;
    private final ApplicationPaymentServiceOutputPort paymentService;
    private final ApplicationInventoryServiceOutputPort inventoryService;
    private final ApplicationNotificationOutputPort notificationService;
    private final ApplicationEventOutputPort eventPublisher;
    private final DomainOutputPortReturnRepository returnRepository;
    private final DomainOutputPortOrderRepository orderRepository;

    public ApplicationReturnService(
            DomainInputPortReturnService domainReturnService,
            ApplicationPaymentServiceOutputPort paymentService,
            ApplicationInventoryServiceOutputPort inventoryService,
            ApplicationNotificationOutputPort notificationService,
            ApplicationEventOutputPort eventPublisher,
            DomainOutputPortReturnRepository returnRepository,
            DomainOutputPortOrderRepository orderRepository) {
        this.domainReturnService = domainReturnService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.returnRepository = returnRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public SharedReturnResponseDTO requestReturn(SharedReturnRequestParams params) {
        try {
            log.info("Processing return request for order: {}", params.orderId());
            
            // Get order snapshot
            SharedOrderSnapshotDTO snapshot = orderRepository.findOrderSnapshot(params.orderId());
            if (snapshot == null) {
                throw new ApplicationTransactionalException(
                    "Order not found: " + params.orderId(), 
                    params.orderId(),
                    "requestReturn"
                );
            }
            
            // Convert params to domain entities
            List<DomainReturnItemEntity> items = params.items().stream()
                    .map(itemParam -> convertToDomainItem(itemParam, snapshot))
                    .collect(Collectors.toList());
            
            // Request return through domain service
            SharedReturnReasonEnum reason = SharedReturnReasonEnum.valueOf(params.reason().toUpperCase());
            DomainReturnRequestEntity returnEntity = domainReturnService.requestReturn(
                    snapshot,
                    items,
                    reason
            );
            
            // Send notification and publish event
            sendNotification(returnEntity, "REQUESTED");
            publishDomainEvent(returnEntity, "ReturnRequested");
            
            log.info("Successfully created return request: {} for order: {}", 
                returnEntity.getReturnId(), params.orderId());
                
            return mapToResponse(returnEntity);
            
        } catch (ApplicationTransactionalException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing return request for order: {}", params.orderId(), ex);
            throw new ApplicationTransactionalException(
                "Failed to request return: " + ex.getMessage(),
                params.orderId(),
                "requestReturn"
            );
        }
    }

    @Override
    public SharedReturnResponseDTO getReturn(String returnId) {
        try {
            DomainReturnRequestEntity returnEntity = returnRepository.findById(returnId);
            if (returnEntity == null) {
                throw new ApplicationTransactionalException(
                    "Return not found: " + returnId,
                    returnId,
                    "getReturn"
                );
            }
            return mapToResponse(returnEntity);
        } catch (Exception ex) {
            log.error("Error retrieving return: {}", returnId, ex);
            throw new ApplicationTransactionalException(
                "Failed to retrieve return: " + ex.getMessage(),
                returnId,
                "getReturn"
            );
        }
    }

    @Override
    public List<SharedReturnResponseDTO> getReturnsByOrder(String orderId) {
        try {
            return returnRepository.findByOrderId(orderId).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error retrieving returns for order: {}", orderId, ex);
            throw new ApplicationTransactionalException(
                "Failed to retrieve returns for order: " + ex.getMessage(),
                orderId,
                "getReturnsByOrder"
            );
        }
    }

    @Override
    public SharedReturnResponseDTO updateReturnStatus(String returnId, SharedReturnStatusEnum status) {
        try {
            log.info("Updating return status: {} to {}", returnId, status);
            
            DomainReturnRequestEntity updated = domainReturnService.updateReturnStatus(returnId, status);
            
            // Coordinate external services based on status
            try {
                if (status == SharedReturnStatusEnum.RECEIVED) {
                    coordinateInventoryUpdate(updated);
                    sendNotification(updated, "RECEIVED");
                }
                
                if (status == SharedReturnStatusEnum.REFUNDED) {
                    coordinateReturnRefund(updated);
                    sendNotification(updated, "REFUNDED");
                }
                
                publishDomainEvent(updated, "ReturnStatusUpdated");
                
            } catch (Exception ex) {
                log.error("Failed to coordinate external services for return status update: {}", returnId, ex);
                throw new ApplicationSagaException(
                    "Failed to complete return status update saga",
                    returnId,
                    "coordinateExternalServices",
                    ex
                );
            }
            
            return mapToResponse(updated);
            
        } catch (ApplicationSagaException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update return status: {}", returnId, ex);
            throw new ApplicationTransactionalException(
                "Failed to update return status: " + ex.getMessage(),
                returnId, 
                "updateReturnStatus"
            );
        }
    }

    private DomainReturnItemEntity convertToDomainItem(SharedReturnItemParams params, SharedOrderSnapshotDTO snapshot) {
        // Find the product ID from the order snapshot
        String productId = snapshot.getItems().stream()
                .filter(item -> item.getOrderItemId().equals(params.orderItemId()))
                .map(SharedOrderItemDTO::getProductId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Order item not found: " + params.orderItemId()));
        
        // Use the public constructor to create the entity
        return new DomainReturnItemEntity(
                params.orderItemId(),
                productId,
                params.quantity(),
                params.reason(),
                params.condition()
        );
    }

    private void coordinateInventoryUpdate(DomainReturnRequestEntity returnEntity) {
        try {
            SharedReturnRequestDTO dto = returnEntity.toDTO();
            inventoryService.incrementStock(dto);
            log.info("Updated inventory for return: {}", returnEntity.getReturnId());
        } catch (Exception ex) {
            log.error("Failed to coordinate inventory update for return: {}", returnEntity.getReturnId(), ex);
            throw ex;
        }
    }

    private void coordinateReturnRefund(DomainReturnRequestEntity returnEntity) {
        try {
            SharedRefundRequestDTO refundRequest = new SharedRefundRequestDTO();
            refundRequest.setRefundId(UUID.randomUUID().toString());
            refundRequest.setOrderId(returnEntity.getOrderId());
            refundRequest.setCancellationId(null);
            refundRequest.setReturnId(returnEntity.getReturnId());
            refundRequest.setAmount(returnEntity.getRefundAmount());
            refundRequest.setReason(returnEntity.getReason().name());
            refundRequest.setStatus("INITIATED");
            refundRequest.setRequestedAt(LocalDateTime.now());
            refundRequest.setProcessedAt(null);
            refundRequest.setMetadata(Map.of(
                "returnId", returnEntity.getReturnId(),
                "rmaNumber", returnEntity.getRmaNumber(),
                "returnStatus", returnEntity.getStatus().name()
            ));
            
            paymentService.initiateRefund(refundRequest);
            log.info("Initiated refund for return: {}, refund ID: {}", 
                returnEntity.getReturnId(), refundRequest.getRefundId());
                
        } catch (Exception ex) {
            log.error("Failed to coordinate refund for return: {}", returnEntity.getReturnId(), ex);
            throw ex;
        }
    }

    private void sendNotification(DomainReturnRequestEntity returnEntity, String eventType) {
        try {
            SharedNotificationDTO notification = new SharedNotificationDTO();
            notification.setNotificationId(UUID.randomUUID().toString());
            notification.setRecipientId(returnEntity.getCustomerId());
            notification.setRecipientEmail(null); // Would be resolved from customer service
            notification.setType("ORDER_RETURN");
            notification.setSubject("Order Return " + eventType);
            notification.setBody(String.format(
                "Your return request for order %s has been %s. RMA: %s",
                returnEntity.getOrderId(),
                eventType.toLowerCase(),
                returnEntity.getRmaNumber()
            ));
            notification.setPriority("NORMAL");
            notification.setMetadata(Map.of(
                "orderId", returnEntity.getOrderId(),
                "returnId", returnEntity.getReturnId(),
                "rmaNumber", returnEntity.getRmaNumber()
            ));
            notification.setScheduledAt(LocalDateTime.now());
            
            notificationService.sendNotification(notification);
        } catch (Exception ex) {
            log.warn("Failed to send notification for return: {}", returnEntity.getReturnId(), ex);
            // Notification failures shouldn't break the main flow
        }
    }

    private void publishDomainEvent(DomainReturnRequestEntity returnEntity, String eventType) {
        try {
            SharedDomainEventDTO event = new SharedDomainEventDTO();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(returnEntity.getReturnId());
            event.setEventType(eventType);
            event.setEventData(Map.of(
                    "orderId", returnEntity.getOrderId(),
                    "returnId", returnEntity.getReturnId(),
                    "rmaNumber", returnEntity.getRmaNumber(),
                    "status", returnEntity.getStatus().name(),
                    "reason", returnEntity.getReason().name(),
                    "customerId", returnEntity.getCustomerId()
            ));
            event.setTimestamp(LocalDateTime.now());
            event.setVersion(returnEntity.getVersion() != null ? returnEntity.getVersion().intValue() : 1);
            event.setSource("ApplicationReturnService");
            event.setCorrelationId(returnEntity.getOrderId());
            
            eventPublisher.publishToKafka(event);
            log.debug("Published domain event: {} for return: {}", eventType, returnEntity.getReturnId());
        } catch (Exception ex) {
            log.warn("Failed to publish domain event for return: {}", returnEntity.getReturnId(), ex);
            // Event publishing failures shouldn't break the main flow
        }
    }

    private SharedReturnResponseDTO mapToResponse(DomainReturnRequestEntity returnEntity) {
        SharedReturnResponseDTO response = new SharedReturnResponseDTO();
        response.setReturnId(returnEntity.getReturnId());
        response.setOrderId(returnEntity.getOrderId());
        response.setRmaNumber(returnEntity.getRmaNumber());
        response.setStatus(returnEntity.getStatus().name());
        response.setRequestedAt(returnEntity.getRequestedAt());
        response.setReturnInstructions(returnEntity.getInstructions());
        response.setReturnAddress(returnEntity.getReturnAddress());
        response.setEstimatedRefund(returnEntity.getRefundAmount());
        response.setSuccess(true);
        response.setMessage("Return processed successfully");
        return response;
    }
}