package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationCompensationException;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.domain.ports.DomainOutputPortOrderEventRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.ports.DomainOutputPortPaymentDetailsRepository;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.ports.DomainOutputPortShippingDetailsRepository;
import ai.shreds.domain.ports.DomainOutputPortShippingService;
import ai.shreds.shared.dtos.PaymentResult;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.enums.SharedEventTypeEnum;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedSagaStepEnum;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for executing compensation actions in case of saga failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationCompensationService {

    private final DomainOutputPortPaymentService paymentService;
    private final DomainOutputPortShippingService shippingService;
    private final DomainOutputPortInventoryService inventoryService;
    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainOutputPortPaymentDetailsRepository paymentDetailsRepository;
    private final DomainOutputPortShippingDetailsRepository shippingDetailsRepository;
    private final DomainOutputPortOrderEventRepository orderEventRepository;

    /**
     * Compensate a payment by issuing a refund.
     */
    public boolean compensatePayment(String transactionId, SharedMoneyValue amount) {
        try {
            log.info("Compensating payment for transaction: {}, amount: {}", transactionId, amount);
            
            PaymentResult result = paymentService.refund(transactionId, amount);
            if (!Boolean.TRUE.equals(result.getSuccess())) {
                log.error("Payment refund failed for transaction: {}, error: {}", 
                    transactionId, result.getErrorMessage());
                throw new ApplicationCompensationException(
                    "Refund failed for transaction: " + transactionId + ", error: " + result.getErrorMessage(), 
                    null
                );
            }
            
            log.info("Payment compensation successful for transaction: {}", transactionId);
            return true;
        } catch (ApplicationCompensationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during payment compensation for transaction: {}", transactionId, e);
            throw new ApplicationCompensationException(
                "Unexpected error during payment compensation: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Compensate shipping by canceling the shipment.
     */
    public boolean compensateShipping(String trackingNumber) {
        try {
            log.info("Compensating shipping for tracking number: {}", trackingNumber);
            
            boolean success = shippingService.cancelShipment(trackingNumber);
            if (!success) {
                log.error("Shipping cancellation failed for tracking number: {}", trackingNumber);
                throw new ApplicationCompensationException(
                    "Cancel shipment failed for tracking number: " + trackingNumber, 
                    null
                );
            }
            
            log.info("Shipping compensation successful for tracking number: {}", trackingNumber);
            return true;
        } catch (ApplicationCompensationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during shipping compensation for tracking number: {}", trackingNumber, e);
            throw new ApplicationCompensationException(
                "Unexpected error during shipping compensation: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Compensate inventory by releasing allocated items.
     */
    public boolean compensateInventory(UUID orderId, List<SharedOrderItemDTO> items) {
        try {
            log.info("Compensating inventory for order: {}, items count: {}", orderId, items.size());
            
            DomainOrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ApplicationCompensationException(
                        "Order not found for inventory compensation: " + orderId, 
                        null
                    ));
            
            List<DomainOrderItemEntity> domainItems = order.getOrderItems().stream()
                    .filter(item -> items.stream()
                            .anyMatch(dto -> dto.getOrderItemId().equals(item.getOrderItemId())))
                    .collect(Collectors.toList());
            
            boolean success = inventoryService.releaseItems(order.getOrderId(), domainItems);
            if (!success) {
                log.error("Inventory release failed for order: {}", orderId);
                throw new ApplicationCompensationException(
                    "Inventory release failed for order: " + orderId, 
                    null
                );
            }
            
            log.info("Inventory compensation successful for order: {}", orderId);
            return true;
        } catch (ApplicationCompensationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during inventory compensation for order: {}", orderId, e);
            throw new ApplicationCompensationException(
                "Unexpected error during inventory compensation: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Execute the complete compensation chain for a failed saga.
     * Compensations are executed in reverse order of the saga steps.
     */
    public void executeCompensationChain(DomainOrderEntity order, String failedStep) {
        List<String> failedCompensations = new ArrayList<>();
        
        try {
            log.info("Executing compensation chain for order: {}, failed step: {}", 
                order.getOrderId(), failedStep);
            
            SharedSagaStepEnum sagaStep = SharedSagaStepEnum.valueOf(failedStep);
            
            // Execute compensations in reverse order based on the step that failed
            switch (sagaStep) {
                case SHIPPING_ARRANGEMENT:
                    // If shipping failed, compensate shipping, then payment, then inventory
                    compensateShippingStep(order, failedCompensations);
                    compensatePaymentStep(order, failedCompensations);
                    compensateInventoryStep(order, failedCompensations);
                    break;
                    
                case PAYMENT_CAPTURE:
                    // If payment capture failed, compensate payment, then inventory
                    compensatePaymentStep(order, failedCompensations);
                    compensateInventoryStep(order, failedCompensations);
                    break;
                    
                case PAYMENT_AUTHORIZATION:
                    // If payment authorization failed, only compensate inventory
                    compensateInventoryStep(order, failedCompensations);
                    break;
                    
                case INVENTORY_ALLOCATION:
                    // If inventory allocation failed, no compensation needed
                    log.info("No compensation needed for inventory allocation failure");
                    break;
                    
                default:
                    // For unknown steps, try to compensate everything
                    log.warn("Unknown failed step: {}, attempting full compensation", failedStep);
                    compensateShippingStep(order, failedCompensations);
                    compensatePaymentStep(order, failedCompensations);
                    compensateInventoryStep(order, failedCompensations);
            }
            
            // Update order status to cancelled
            order.transitionTo(SharedOrderStatusEnum.CANCELLED);
            orderRepository.save(order);
            
            // Create compensation event
            DomainOrderEventEntity compensationEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                SharedEventTypeEnum.ORDER_CANCELLED,
                "Compensation executed for failed step: " + failedStep +
                (failedCompensations.isEmpty() ? "" : ", failed compensations: " + String.join(", ", failedCompensations))
            );
            orderEventRepository.save(compensationEvent);
            
            if (!failedCompensations.isEmpty()) {
                log.error("Compensation chain completed with {} failures for order: {}", 
                    failedCompensations.size(), order.getOrderId());
                throw new ApplicationCompensationException(
                    "Partial compensation failure for order: " + order.getOrderId(), 
                    null, 
                    failedCompensations
                );
            }
            
            log.info("Compensation chain completed successfully for order: {}", order.getOrderId());
            
        } catch (ApplicationCompensationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Critical error in compensation chain for order: {}", order.getOrderId(), ex);
            throw new ApplicationCompensationException(
                "Compensation chain execution failed for order: " + order.getOrderId(), 
                ex, 
                failedCompensations
            );
        }
    }

    private void compensateShippingStep(DomainOrderEntity order, List<String> failedCompensations) {
        try {
            Optional<DomainShippingDetailsEntity> shippingDetails = 
                shippingDetailsRepository.findByOrderId(order.getOrderId());
            
            if (shippingDetails.isPresent() && shippingDetails.get().getTrackingNumber() != null) {
                compensateShipping(shippingDetails.get().getTrackingNumber());
            } else {
                log.debug("No shipping details found for order: {}, skipping shipping compensation", order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Shipping compensation failed for order: {}", order.getOrderId(), e);
            failedCompensations.add("SHIPPING_COMPENSATION");
        }
    }

    private void compensatePaymentStep(DomainOrderEntity order, List<String> failedCompensations) {
        try {
            Optional<DomainPaymentDetailsEntity> paymentDetails = 
                paymentDetailsRepository.findByOrderId(order.getOrderId());
            
            if (paymentDetails.isPresent() && paymentDetails.get().getTransactionId() != null) {
                compensatePayment(
                    paymentDetails.get().getTransactionId(), 
                    paymentDetails.get().getAmount()
                );
            } else {
                log.debug("No payment details found for order: {}, skipping payment compensation", order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Payment compensation failed for order: {}", order.getOrderId(), e);
            failedCompensations.add("PAYMENT_COMPENSATION");
        }
    }

    private void compensateInventoryStep(DomainOrderEntity order, List<String> failedCompensations) {
        try {
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                List<SharedOrderItemDTO> sharedItems = order.getOrderItems().stream()
                    .map(DomainOrderItemEntity::toSharedDTO)
                    .collect(Collectors.toList());
                
                compensateInventory(order.getOrderId(), sharedItems);
            } else {
                log.debug("No order items found for order: {}, skipping inventory compensation", order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Inventory compensation failed for order: {}", order.getOrderId(), e);
            failedCompensations.add("INVENTORY_COMPENSATION");
        }
    }

    /**
     * Check if compensation is needed for a specific saga step.
     */
    public boolean isCompensationNeeded(SharedSagaStepEnum step) {
        switch (step) {
            case PAYMENT_AUTHORIZATION:
            case PAYMENT_CAPTURE:
            case SHIPPING_ARRANGEMENT:
            case INVENTORY_ALLOCATION:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the compensation steps required for a given saga step.
     */
    public List<String> getCompensationSteps(SharedSagaStepEnum failedStep) {
        List<String> steps = new ArrayList<>();
        
        switch (failedStep) {
            case SHIPPING_ARRANGEMENT:
                steps.add("SHIPPING_COMPENSATION");
                steps.add("PAYMENT_COMPENSATION");
                steps.add("INVENTORY_COMPENSATION");
                break;
            case PAYMENT_CAPTURE:
                steps.add("PAYMENT_COMPENSATION");
                steps.add("INVENTORY_COMPENSATION");
                break;
            case PAYMENT_AUTHORIZATION:
                steps.add("INVENTORY_COMPENSATION");
                break;
            case INVENTORY_ALLOCATION:
                // No compensation needed
                break;
            default:
                steps.add("UNKNOWN_COMPENSATION");
        }
        
        return steps;
    }
}