package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationSagaException;
import ai.shreds.application.exceptions.ApplicationTransactionalException;
import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.application.ports.ApplicationEventOutputPort;
import ai.shreds.application.ports.ApplicationInventoryServiceOutputPort;
import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.application.ports.ApplicationNotificationOutputPort;
import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.domain.ports.DomainInputPortCancellationService;
import ai.shreds.domain.ports.DomainOutputPortCancellationRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.dtos.SharedDomainEventDTO;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedSystemCancellationMessage;
import ai.shreds.shared.dtos.SharedNotificationDTO;
import ai.shreds.shared.value_objects.SharedCancellationRequestParams;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for handling cancellation use-cases.
 * Orchestrates domain logic with external services and manages transactions.
 */
@Slf4j
@Service
@Transactional
public class ApplicationCancellationService implements ApplicationCancellationInputPort {

    private final DomainInputPortCancellationService domainCancellationService;
    private final ApplicationPaymentServiceOutputPort paymentService;
    private final ApplicationInventoryServiceOutputPort inventoryService;
    private final ApplicationNotificationOutputPort notificationService;
    private final ApplicationEventOutputPort eventPublisher;
    private final DomainOutputPortCancellationRepository cancellationRepository;
    private final DomainOutputPortOrderRepository orderRepository;

    public ApplicationCancellationService(
            DomainInputPortCancellationService domainCancellationService,
            ApplicationPaymentServiceOutputPort paymentService,
            ApplicationInventoryServiceOutputPort inventoryService,
            ApplicationNotificationOutputPort notificationService,
            ApplicationEventOutputPort eventPublisher,
            DomainOutputPortCancellationRepository cancellationRepository,
            DomainOutputPortOrderRepository orderRepository) {
        this.domainCancellationService = domainCancellationService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.cancellationRepository = cancellationRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public SharedCancellationResponseDTO requestCancellation(SharedCancellationRequestParams params) {
        try {
            log.info("Processing cancellation request for order: {}", params.orderId());
            
            // Get order snapshot
            SharedOrderSnapshotDTO snapshot = orderRepository.findOrderSnapshot(params.orderId());
            if (snapshot == null) {
                throw new ApplicationTransactionalException(
                    "Order not found: " + params.orderId(), 
                    params.orderId(),
                    "requestCancellation"
                );
            }
            
            // Request cancellation through domain service
            SharedCancellationReasonEnum reason = SharedCancellationReasonEnum.valueOf(params.reason().toUpperCase());
            DomainCancellationRequestEntity cancellation = domainCancellationService.requestCancellation(
                    snapshot,
                    reason,
                    params.notes()
            );
            
            // Coordinate external services in saga pattern
            try {
                coordinateInventoryRelease(cancellation);
                coordinateRefund(cancellation);
                sendNotification(cancellation, "REQUESTED");
                publishDomainEvent(cancellation, "CancellationRequested");
            } catch (Exception ex) {
                log.error("Failed to coordinate external services for cancellation: {}", cancellation.getCancellationId(), ex);
                // Compensation would be handled here
                throw new ApplicationSagaException(
                    "Failed to complete cancellation saga",
                    cancellation.getCancellationId(),
                    "coordinateExternalServices",
                    ex
                );
            }
            
            return mapToResponse(cancellation);
            
        } catch (ApplicationTransactionalException | ApplicationSagaException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing cancellation request for order: {}", params.orderId(), ex);
            throw new ApplicationTransactionalException(
                "Failed to request cancellation: " + ex.getMessage(),
                params.orderId(),
                "requestCancellation"
            );
        }
    }

    @Override
    public SharedCancellationResponseDTO getCancellation(String cancellationId) {
        try {
            DomainCancellationRequestEntity cancellation = cancellationRepository.findById(cancellationId);
            if (cancellation == null) {
                throw new ApplicationTransactionalException(
                    "Cancellation not found: " + cancellationId,
                    cancellationId,
                    "getCancellation"
                );
            }
            return mapToResponse(cancellation);
        } catch (Exception ex) {
            log.error("Error retrieving cancellation: {}", cancellationId, ex);
            throw new ApplicationTransactionalException(
                "Failed to retrieve cancellation: " + ex.getMessage(),
                cancellationId,
                "getCancellation"
            );
        }
    }

    @Override
    public List<SharedCancellationResponseDTO> getCancellationsByOrder(String orderId) {
        try {
            return cancellationRepository.findByOrderId(orderId).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error retrieving cancellations for order: {}", orderId, ex);
            throw new ApplicationTransactionalException(
                "Failed to retrieve cancellations for order: " + ex.getMessage(),
                orderId,
                "getCancellationsByOrder"
            );
        }
    }

    @Override
    public void processSystemCancellation(SharedSystemCancellationMessage message) {
        try {
            log.info("Processing system cancellation for order: {}, reason: {}", message.getOrderId(), message.getReason());
            
            // Create request params from system message
            SharedCancellationRequestParams params = new SharedCancellationRequestParams(
                message.getOrderId(),
                message.getReason(),
                "System cancellation: " + message.getReason()
            );
            
            requestCancellation(params);
            
        } catch (Exception ex) {
            log.error("Failed to process system cancellation for order: {}", message.getOrderId(), ex);
            // System cancellations are critical, so we might need different handling
            throw new ApplicationTransactionalException(
                "Failed to process system cancellation: " + ex.getMessage(),
                message.getOrderId(),
                "processSystemCancellation"
            );
        }
    }

    @Override
    public void completeCancellation(String cancellationId) {
        try {
            log.info("Completing cancellation: {}", cancellationId);
            
            DomainCancellationRequestEntity cancellation = domainCancellationService.completeCancellation(cancellationId);
            
            // Ensure external services are coordinated
            coordinateRefund(cancellation);
            coordinateInventoryRelease(cancellation);
            sendNotification(cancellation, "COMPLETED");
            publishDomainEvent(cancellation, "CancellationCompleted");
            
            log.info("Successfully completed cancellation: {}", cancellationId);
            
        } catch (Exception ex) {
            log.error("Failed to complete cancellation: {}", cancellationId, ex);
            throw new ApplicationSagaException(
                "Failed to complete cancellation saga: " + ex.getMessage(),
                cancellationId,
                "completeCancellation",
                ex
            );
        }
    }

    private SharedCancellationResponseDTO mapToResponse(DomainCancellationRequestEntity cancellation) {
        SharedCancellationResponseDTO response = new SharedCancellationResponseDTO();
        response.setCancellationId(cancellation.getCancellationId());
        response.setOrderId(cancellation.getOrderId());
        response.setStatus(cancellation.getStatus().name());
        response.setReason(cancellation.getReason().name());
        response.setRequestedAt(cancellation.getRequestedAt());
        response.setRefundAmount(cancellation.getRefundAmount());
        response.setMessage("Cancellation processed successfully");
        response.setSuccess(true);
        return response;
    }

    private void coordinateRefund(DomainCancellationRequestEntity cancellation) {
        try {
            SharedRefundRequestDTO refundRequest = new SharedRefundRequestDTO();
            refundRequest.setRefundId(UUID.randomUUID().toString());
            refundRequest.setOrderId(cancellation.getOrderId());
            refundRequest.setCancellationId(cancellation.getCancellationId());
            refundRequest.setReturnId(null);
            refundRequest.setAmount(cancellation.getRefundAmount());
            refundRequest.setReason(cancellation.getReason().name());
            refundRequest.setStatus("INITIATED");
            refundRequest.setRequestedAt(LocalDateTime.now());
            refundRequest.setProcessedAt(null);
            refundRequest.setMetadata(Map.of(
                "cancellationId", cancellation.getCancellationId(),
                "orderStatus", cancellation.getStatus().name()
            ));
            
            paymentService.initiateRefund(refundRequest);
            log.info("Initiated refund for cancellation: {}, refund ID: {}", 
                cancellation.getCancellationId(), refundRequest.getRefundId());
            
        } catch (Exception ex) {
            log.error("Failed to coordinate refund for cancellation: {}", cancellation.getCancellationId(), ex);
            throw ex;
        }
    }

    private void coordinateInventoryRelease(DomainCancellationRequestEntity cancellation) {
        try {
            SharedOrderSnapshotDTO snapshot = orderRepository.findOrderSnapshot(cancellation.getOrderId());
            inventoryService.releaseReservedStock(snapshot);
            log.info("Released inventory for cancelled order: {}", cancellation.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to coordinate inventory release for order: {}", cancellation.getOrderId(), ex);
            throw ex;
        }
    }

    private void sendNotification(DomainCancellationRequestEntity cancellation, String eventType) {
        try {
            SharedNotificationDTO notification = new SharedNotificationDTO();
            notification.setNotificationId(UUID.randomUUID().toString());
            notification.setRecipientId(cancellation.getCustomerId());
            notification.setRecipientEmail(null); // Would be resolved from customer service
            notification.setType("ORDER_CANCELLATION");
            notification.setSubject("Order Cancellation " + eventType);
            notification.setBody(String.format(
                "Your order %s has been %s. Cancellation ID: %s",
                cancellation.getOrderId(),
                eventType.toLowerCase(),
                cancellation.getCancellationId()
            ));
            notification.setPriority("HIGH");
            notification.setMetadata(Map.of(
                "orderId", cancellation.getOrderId(),
                "cancellationId", cancellation.getCancellationId()
            ));
            notification.setScheduledAt(LocalDateTime.now());
            
            notificationService.sendNotification(notification);
        } catch (Exception ex) {
            log.warn("Failed to send notification for cancellation: {}", cancellation.getCancellationId(), ex);
            // Notification failures shouldn't break the main flow
        }
    }

    private void publishDomainEvent(DomainCancellationRequestEntity cancellation, String eventType) {
        try {
            SharedDomainEventDTO event = new SharedDomainEventDTO();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(cancellation.getCancellationId());
            event.setEventType(eventType);
            event.setEventData(Map.of(
                    "orderId", cancellation.getOrderId(),
                    "cancellationId", cancellation.getCancellationId(),
                    "status", cancellation.getStatus().name(),
                    "reason", cancellation.getReason().name(),
                    "customerId", cancellation.getCustomerId()
            ));
            event.setTimestamp(LocalDateTime.now());
            event.setVersion(cancellation.getVersion() != null ? cancellation.getVersion().intValue() : 1);
            event.setSource("ApplicationCancellationService");
            event.setCorrelationId(cancellation.getOrderId());
            
            eventPublisher.publishToKafka(event);
            log.debug("Published domain event: {} for cancellation: {}", eventType, cancellation.getCancellationId());
        } catch (Exception ex) {
            log.warn("Failed to publish domain event for cancellation: {}", cancellation.getCancellationId(), ex);
            // Event publishing failures shouldn't break the main flow
        }
    }
}