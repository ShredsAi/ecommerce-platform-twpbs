package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.domain.ports.*;
import ai.shreds.shared.enums.SharedSagaStepEnum;
import ai.shreds.shared.enums.SharedSagaStatusEnum;
import ai.shreds.shared.enums.SharedEventTypeEnum;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import ai.shreds.shared.dtos.PaymentResult;
import ai.shreds.shared.dtos.ShipmentResult;
import ai.shreds.domain.exceptions.DomainSagaException;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service implementing saga orchestration logic.
 * This service coordinates the saga workflow across different steps.
 */
@Service
public class DomainSagaOrchestrationService implements DomainInputPortSagaCoordination {

    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainOutputPortSagaStateRepository sagaStateRepository;
    private final DomainOutputPortOrderEventRepository orderEventRepository;
    private final DomainOutputPortPaymentService paymentService;
    private final DomainOutputPortShippingService shippingService;
    private final DomainOutputPortInventoryService inventoryService;
    private final DomainOutputPortNotificationService notificationService;
    private final DomainOutputPortEventPublisher eventPublisher;

    public DomainSagaOrchestrationService(DomainOutputPortOrderRepository orderRepository,
                                        DomainOutputPortSagaStateRepository sagaStateRepository,
                                        DomainOutputPortOrderEventRepository orderEventRepository,
                                        DomainOutputPortPaymentService paymentService,
                                        DomainOutputPortShippingService shippingService,
                                        DomainOutputPortInventoryService inventoryService,
                                        DomainOutputPortNotificationService notificationService,
                                        DomainOutputPortEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.sagaStateRepository = sagaStateRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DomainSagaStateEntity initiateSaga(DomainOrderEntity order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        try {
            // Create new saga state
            DomainSagaStateEntity sagaState = DomainSagaStateEntity.createNew(order.getOrderId());
            
            // Save saga state
            sagaState = sagaStateRepository.save(sagaState);
            
            // Create saga initiation event
            DomainOrderEventEntity initiationEvent = DomainOrderEventEntity.create(
                order.getOrderId(),
                SharedEventTypeEnum.ORDER_CREATED,
                "Saga initiated for order",
                order.getOrderStatus(),
                order.getOrderStatus()
            );
            orderEventRepository.save(initiationEvent);
            
            // Publish saga initiated event
            eventPublisher.publishFulfillmentEvent(
                sagaState.getSagaId(),
                order.getOrderId().getValue(),
                sagaState.getCurrentStep().name(),
                sagaState.getStatus().name()
            );
            
            // Start the first step - payment authorization
            processStep(sagaState, SharedSagaStepEnum.PAYMENT_AUTHORIZATION);
            
            return sagaState;
            
        } catch (Exception e) {
            throw new DomainSagaException(
                "Failed to initiate saga for order: " + order.getOrderId().getValue(),
                null,
                order.getOrderId().getValue(),
                e
            );
        }
    }

    @Override
    public void processStep(DomainSagaStateEntity sagaState, SharedSagaStepEnum step) {
        if (sagaState == null) {
            throw new IllegalArgumentException("Saga state cannot be null");
        }
        if (step == null) {
            throw new IllegalArgumentException("Step cannot be null");
        }

        try {
            // Update saga state to current step
            sagaState.updateStep(step);
            sagaStateRepository.save(sagaState);
            
            // Get the order for processing
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId().getValue())
                .orElseThrow(() -> new DomainSagaException(
                    "Order not found for saga",
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue()
                ));
            
            boolean stepSuccess = false;
            
            // Execute the appropriate step
            switch (step) {
                case PAYMENT_AUTHORIZATION:
                    stepSuccess = executePaymentAuthorization(order, sagaState);
                    break;
                case PAYMENT_CAPTURE:
                    stepSuccess = executePaymentCapture(order, sagaState);
                    break;
                case SHIPPING_ARRANGEMENT:
                    stepSuccess = executeShippingArrangement(order, sagaState);
                    break;
                case INVENTORY_ALLOCATION:
                    stepSuccess = executeInventoryAllocation(order, sagaState);
                    break;
                case NOTIFICATION_SENDING:
                    stepSuccess = executeNotificationSending(order, sagaState);
                    break;
                default:
                    throw new DomainSagaException(
                        "Unknown saga step: " + step.name(),
                        sagaState.getSagaId(),
                        sagaState.getOrderId().getValue()
                    );
            }
            
            if (stepSuccess) {
                // Create success event
                DomainOrderEventEntity successEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    getEventTypeForStep(step),
                    "Step " + step.name() + " completed successfully",
                    order.getOrderStatus(),
                    order.getOrderStatus()
                );
                orderEventRepository.save(successEvent);
                
                // Move to next step or complete saga
                moveToNextStep(sagaState);
            } else {
                // Create failure event
                DomainOrderEventEntity failureEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    getFailureEventTypeForStep(step),
                    "Step " + step.name() + " failed",
                    order.getOrderStatus(),
                    order.getOrderStatus()
                );
                orderEventRepository.save(failureEvent);
                
                // Step failed, initiate compensation
                compensate(sagaState, step);
            }
            
        } catch (Exception e) {
            throw new DomainSagaException(
                "Failed to process saga step: " + step.name(),
                sagaState.getSagaId(),
                sagaState.getOrderId().getValue(),
                e
            );
        }
    }

    @Override
    public void compensate(DomainSagaStateEntity sagaState, SharedSagaStepEnum failedStep) {
        if (sagaState == null) {
            throw new IllegalArgumentException("Saga state cannot be null");
        }

        try {
            sagaState.markCompensating();
            sagaStateRepository.save(sagaState);
            
            // Get the order for compensation
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId().getValue())
                .orElseThrow(() -> new DomainSagaException(
                    "Order not found for compensation",
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue()
                ));
            
            // Execute compensation logic based on failed step
            boolean compensationSuccess = executeCompensation(sagaState, failedStep, order);
            
            if (compensationSuccess) {
                sagaState.markFailed();
                
                // Update order status to cancelled
                order.transitionTo(SharedOrderStatusEnum.CANCELLED);
                orderRepository.save(order);
                
                // Create compensation success event
                DomainOrderEventEntity compensationEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    SharedEventTypeEnum.ORDER_CANCELLED,
                    "Compensation completed successfully for failed step: " + failedStep.name(),
                    order.getOrderStatus(),
                    SharedOrderStatusEnum.CANCELLED
                );
                orderEventRepository.save(compensationEvent);
                
                eventPublisher.publishFulfillmentEvent(
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue(),
                    "COMPENSATION_COMPLETED",
                    "FAILED"
                );
                
                // Notify customer of cancellation
                notificationService.sendCancellationNotification(
                    order.getCustomerId().getValue(),
                    order.getOrderId().getValue().toString(),
                    "Order cancelled due to processing failure"
                );
            } else {
                // Compensation failed - requires manual intervention
                eventPublisher.publishSagaTimeoutEvent(
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue(),
                    "COMPENSATION_FAILED",
                    0
                );
            }
            
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            throw new DomainSagaException(
                "Failed to compensate saga for step: " + failedStep.name(),
                sagaState.getSagaId(),
                sagaState.getOrderId().getValue(),
                e
            );
        }
    }

    @Override
    public void handleTimeout(DomainSagaStateEntity sagaState) {
        if (sagaState == null) {
            throw new IllegalArgumentException("Saga state cannot be null");
        }

        try {
            if (sagaState.canRetry()) {
                // Retry current step
                sagaState.incrementRetryCount();
                sagaStateRepository.save(sagaState);
                
                // Create timeout retry event
                DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId().getValue())
                    .orElseThrow(() -> new DomainSagaException(
                        "Order not found for timeout handling",
                        sagaState.getSagaId(),
                        sagaState.getOrderId().getValue()
                    ));
                
                DomainOrderEventEntity timeoutEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    SharedEventTypeEnum.TIMEOUT_HANDLED,
                    "Timeout handled, retrying step: " + sagaState.getCurrentStep().name() + ", retry count: " + sagaState.getRetryCount(),
                    order.getOrderStatus(),
                    order.getOrderStatus()
                );
                orderEventRepository.save(timeoutEvent);
                
                processStep(sagaState, sagaState.getCurrentStep());
            } else {
                // Max retries exceeded
                sagaState.markTimedOut();
                sagaStateRepository.save(sagaState);
                
                // Create timeout exhausted event
                DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId().getValue())
                    .orElseThrow(() -> new DomainSagaException(
                        "Order not found for timeout handling",
                        sagaState.getSagaId(),
                        sagaState.getOrderId().getValue()
                    ));
                
                DomainOrderEventEntity exhaustedEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    SharedEventTypeEnum.SAGA_TIMEOUT_EXHAUSTED,
                    "Saga timeout exhausted after " + sagaState.getRetryCount() + " retries",
                    order.getOrderStatus(),
                    order.getOrderStatus()
                );
                orderEventRepository.save(exhaustedEvent);
                
                eventPublisher.publishSagaTimeoutEvent(
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue(),
                    sagaState.getCurrentStep().name(),
                    sagaState.getRetryCount()
                );
            }
            
        } catch (Exception e) {
            throw new DomainSagaException(
                "Failed to handle saga timeout",
                sagaState.getSagaId(),
                sagaState.getOrderId().getValue(),
                e
            );
        }
    }

    // Private helper methods
    
    private boolean executePaymentAuthorization(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            PaymentResult result = paymentService.authorize(order);
            
            if (result.isSuccess()) {
                // Update order status to CONFIRMED
                order.transitionTo(SharedOrderStatusEnum.CONFIRMED);
                orderRepository.save(order);
                
                // Publish payment event
                eventPublisher.publishPaymentEvent(
                    order.getOrderId().getValue(),
                    result.getTransactionId(),
                    SharedPaymentStatusEnum.AUTHORIZED.name(),
                    result.getAmount().toString()
                );
            }
            
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean executePaymentCapture(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            // Find existing payment details to get transaction ID
            // In real implementation, this would come from payment details repository
            String transactionId = "TXN-" + order.getOrderId().getValue().toString().substring(0, 8);
            
            PaymentResult result = paymentService.capture(transactionId, order.getTotalAmount());
            
            if (result.isSuccess()) {
                // Update order status to PAID
                order.transitionTo(SharedOrderStatusEnum.PAID);
                orderRepository.save(order);
                
                // Publish payment captured event
                eventPublisher.publishPaymentEvent(
                    order.getOrderId().getValue(),
                    result.getTransactionId(),
                    SharedPaymentStatusEnum.CAPTURED.name(),
                    result.getAmount().toString()
                );
            }
            
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean executeShippingArrangement(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            ShipmentResult result = shippingService.createShipment(order);
            
            if (result.isSuccess()) {
                // Update order status to PROCESSING
                order.transitionTo(SharedOrderStatusEnum.PROCESSING);
                orderRepository.save(order);
                
                // Publish shipping event
                eventPublisher.publishShippingEvent(
                    order.getOrderId().getValue(),
                    result.getTrackingNumber(),
                    "ARRANGED",
                    result.getCarrier()
                );
                
                // Subscribe to shipping updates
                shippingService.subscribeToUpdates(order.getOrderId().getValue());
            }
            
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean executeInventoryAllocation(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            boolean allocationResult = inventoryService.allocateItems(
                order.getOrderId().getValue(),
                order.getOrderItems()
            );
            
            if (allocationResult) {
                // Create inventory allocation event
                DomainOrderEventEntity allocationEvent = DomainOrderEventEntity.create(
                    order.getOrderId(),
                    SharedEventTypeEnum.ORDER_CREATED, // Using existing enum value
                    "Inventory allocated successfully for " + order.getOrderItems().size() + " items",
                    order.getOrderStatus(),
                    order.getOrderStatus()
                );
                orderEventRepository.save(allocationEvent);
            }
            
            return allocationResult;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean executeNotificationSending(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            // Prepare notification data
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("orderId", order.getOrderId().getValue().toString());
            notificationData.put("orderNumber", order.getOrderNumber());
            notificationData.put("totalAmount", order.getTotalAmount().getValue().toString());
            notificationData.put("currency", order.getTotalAmount().getCurrency());
            notificationData.put("itemCount", order.getOrderItems().size());
            
            // Send order confirmation notification
            notificationService.sendOrderConfirmation(
                order.getCustomerId().getValue(),
                order.getOrderId().getValue().toString(),
                notificationData
            );
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void moveToNextStep(DomainSagaStateEntity sagaState) {
        SharedSagaStepEnum currentStep = sagaState.getCurrentStep();
        SharedSagaStepEnum nextStep = getNextStep(currentStep);
        
        if (nextStep != null) {
            processStep(sagaState, nextStep);
        } else {
            // Saga completed
            sagaState.markCompleted();
            sagaStateRepository.save(sagaState);
            
            // Get the order and mark as completed
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId().getValue())
                .orElseThrow(() -> new DomainSagaException(
                    "Order not found for saga completion",
                    sagaState.getSagaId(),
                    sagaState.getOrderId().getValue()
                ));
            
            order.transitionTo(SharedOrderStatusEnum.COMPLETED);
            orderRepository.save(order);
            
            // Create completion event
            DomainOrderEventEntity completionEvent = DomainOrderEventEntity.create(
                order.getOrderId(),
                SharedEventTypeEnum.ORDER_DELIVERED,
                "Order fulfillment saga completed successfully",
                order.getOrderStatus(),
                SharedOrderStatusEnum.COMPLETED
            );
            orderEventRepository.save(completionEvent);
            
            eventPublisher.publishFulfillmentEvent(
                sagaState.getSagaId(),
                sagaState.getOrderId().getValue(),
                "SAGA_COMPLETED",
                "COMPLETED"
            );
            
            // Send completion notification
            Map<String, Object> completionData = new HashMap<>();
            completionData.put("orderId", order.getOrderId().getValue().toString());
            completionData.put("completedAt", java.time.Instant.now().toString());
            
            notificationService.sendDeliveryConfirmation(
                order.getCustomerId().getValue(),
                order.getOrderId().getValue().toString(),
                completionData
            );
        }
    }
    
    private SharedSagaStepEnum getNextStep(SharedSagaStepEnum currentStep) {
        switch (currentStep) {
            case PAYMENT_AUTHORIZATION:
                return SharedSagaStepEnum.INVENTORY_ALLOCATION;
            case INVENTORY_ALLOCATION:
                return SharedSagaStepEnum.SHIPPING_ARRANGEMENT;
            case SHIPPING_ARRANGEMENT:
                return SharedSagaStepEnum.PAYMENT_CAPTURE;
            case PAYMENT_CAPTURE:
                return SharedSagaStepEnum.NOTIFICATION_SENDING;
            case NOTIFICATION_SENDING:
                return null; // Saga completed
            default:
                return null;
        }
    }
    
    private boolean executeCompensation(DomainSagaStateEntity sagaState, SharedSagaStepEnum failedStep, DomainOrderEntity order) {
        try {
            boolean allCompensationsSuccessful = true;
            
            // Execute compensations in reverse order of completed steps
            SharedSagaStepEnum currentStep = failedStep;
            
            while (currentStep != null) {
                boolean compensationResult = false;
                
                switch (currentStep) {
                    case NOTIFICATION_SENDING:
                        // No compensation needed for notifications
                        compensationResult = true;
                        break;
                        
                    case PAYMENT_CAPTURE:
                        compensationResult = compensatePaymentCapture(order);
                        break;
                        
                    case SHIPPING_ARRANGEMENT:
                        compensationResult = compensateShippingArrangement(order);
                        break;
                        
                    case INVENTORY_ALLOCATION:
                        compensationResult = compensateInventoryAllocation(order);
                        break;
                        
                    case PAYMENT_AUTHORIZATION:
                        compensationResult = compensatePaymentAuthorization(order);
                        break;
                
                    default:
                        compensationResult = true;
                }
                
                if (!compensationResult) {
                    allCompensationsSuccessful = false;
                }
                
                currentStep = getPreviousStep(currentStep);
            }
            
            return allCompensationsSuccessful;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean compensatePaymentCapture(DomainOrderEntity order) {
        try {
            String transactionId = "TXN-" + order.getOrderId().getValue().toString().substring(0, 8);
            PaymentResult refundResult = paymentService.refund(transactionId, order.getTotalAmount());
            return refundResult.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean compensateShippingArrangement(DomainOrderEntity order) {
        try {
            // Cancel shipment - tracking number would come from shipping details in real implementation
            String trackingNumber = "TRK-" + order.getOrderId().getValue().toString().substring(0, 8);
            return shippingService.cancelShipment(trackingNumber);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean compensateInventoryAllocation(DomainOrderEntity order) {
        try {
            return inventoryService.releaseItems(order.getOrderId().getValue(), order.getOrderItems());
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean compensatePaymentAuthorization(DomainOrderEntity order) {
        try {
            String transactionId = "TXN-" + order.getOrderId().getValue().toString().substring(0, 8);
            PaymentResult cancelResult = paymentService.cancel(transactionId);
            return cancelResult.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    private SharedSagaStepEnum getPreviousStep(SharedSagaStepEnum currentStep) {
        switch (currentStep) {
            case NOTIFICATION_SENDING:
                return SharedSagaStepEnum.PAYMENT_CAPTURE;
            case PAYMENT_CAPTURE:
                return SharedSagaStepEnum.SHIPPING_ARRANGEMENT;
            case SHIPPING_ARRANGEMENT:
                return SharedSagaStepEnum.INVENTORY_ALLOCATION;
            case INVENTORY_ALLOCATION:
                return SharedSagaStepEnum.PAYMENT_AUTHORIZATION;
            case PAYMENT_AUTHORIZATION:
                return null;
            default:
                return null;
        }
    }
    
    private SharedEventTypeEnum getEventTypeForStep(SharedSagaStepEnum step) {
        switch (step) {
            case PAYMENT_AUTHORIZATION:
                return SharedEventTypeEnum.PAYMENT_AUTHORIZED;
            case PAYMENT_CAPTURE:
                return SharedEventTypeEnum.PAYMENT_CAPTURED;
            case SHIPPING_ARRANGEMENT:
                return SharedEventTypeEnum.SHIPPING_ARRANGED;
            case INVENTORY_ALLOCATION:
                return SharedEventTypeEnum.ORDER_CREATED; // Reusing existing enum
            case NOTIFICATION_SENDING:
                return SharedEventTypeEnum.ORDER_DELIVERED;
            default:
                return SharedEventTypeEnum.ORDER_CREATED;
        }
    }
    
    private SharedEventTypeEnum getFailureEventTypeForStep(SharedSagaStepEnum step) {
        switch (step) {
            case PAYMENT_AUTHORIZATION:
            case PAYMENT_CAPTURE:
                return SharedEventTypeEnum.PAYMENT_FAILED;
            case SHIPPING_ARRANGEMENT:
                return SharedEventTypeEnum.SHIPPING_FAILED;
            case INVENTORY_ALLOCATION:
            case NOTIFICATION_SENDING:
            default:
                return SharedEventTypeEnum.ORDER_CANCELLED;
        }
    }
}