package ai.shreds.domain.services;

import ai.shreds.application.dtos.ApplicationInventoryItemDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.domain.entities.DomainAddressEntity;
import ai.shreds.domain.events.DomainOrderCreatedEvent;
import ai.shreds.domain.events.DomainOrderCreationFailedEvent;
import ai.shreds.domain.exceptions.DomainOrderException;
import ai.shreds.domain.exceptions.DomainValidationException;
import ai.shreds.domain.ports.DomainInputPortCreateOrder;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedAddressDTO;
import ai.shreds.shared.dtos.SharedPaymentMethodDTO;
import ai.shreds.shared.enums.SharedErrorTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain service that implements the order creation use case.
 * This service orchestrates the entire order creation workflow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainServiceOrderCreation implements DomainInputPortCreateOrder {
    
    private final DomainServiceOrderFactory orderFactory;
    private final DomainServiceValidation validationService;
    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainOutputPortEventPublisher eventPublisher;
    
    /**
     * Executes the order creation workflow.
     * This method is the main entry point for order creation from the application layer.
     */
    @Override
    @Transactional
    public DomainOrderAggregate execute(String cartId,
                                       String customerId,
                                       List<ApplicationInventoryItemDTO> items,
                                       SharedAddressDTO billingAddress,
                                       SharedAddressDTO shippingAddress,
                                       ApplicationPricingResponseDTO pricing,
                                       SharedPaymentMethodDTO paymentMethod) {
        
        log.info("Starting order creation for cartId: {}, customerId: {}", cartId, customerId);
        
        try {
            // 1. Check for idempotency - ensure we don't create duplicate orders
            checkIdempotency(cartId);
            
            // 2. Validate customer
            validationService.validateCustomer(customerId);
            
            // 3. Convert and validate addresses
            DomainAddressValue billingAddressValue = billingAddress.toDomainValue();
            DomainAddressValue shippingAddressValue = shippingAddress.toDomainValue();
            validationService.validateAddresses(billingAddressValue, shippingAddressValue);
            
            // 4. Convert addresses to entities (they will be persisted)
            DomainAddressEntity billingAddressEntity = billingAddressValue.toEntity();
            DomainAddressEntity shippingAddressEntity = shippingAddressValue.toEntity();
            
            // 5. Validate business rules
            DomainMoneyValue totalMoney = new DomainMoneyValue(pricing.getTotal().getAmount(), pricing.getTotal().getCurrency());
            validationService.validateBusinessRules(items.size(), totalMoney);
            
            // 6. Create order aggregate using factory
            DomainOrderAggregate orderAggregate = orderFactory.create(
                customerId, cartId, items, pricing, 
                billingAddressEntity, shippingAddressEntity, paymentMethod
            );
            
            // 7. Final validation of the created aggregate
            orderAggregate.validateInvariants();
            // Removed calculateTotals() call - factory already sets correct amounts from pricing service
            
            // 8. Validate order items
            validationService.validateOrderItems(orderAggregate.getOrderItems());
            
            // 9. Persist the order aggregate
            DomainOrderAggregate persistedOrder = persistOrder(orderAggregate);
            
            // 10. Publish success event
            publishSuccessEvent(persistedOrder, cartId);
            
            log.info("Order creation completed successfully for cartId: {}, orderId: {}", 
                cartId, persistedOrder.getOrder().getOrderId());
            
            return persistedOrder;
            
        } catch (Exception e) {
            log.error("Order creation failed for cartId: {}, customerId: {}", cartId, customerId, e);
            
            // Publish failure event
            publishFailureEvent(cartId, customerId, e);
            
            // Re-throw the exception
            if (e instanceof DomainOrderException || e instanceof DomainValidationException) {
                throw e;
            } else {
                throw new DomainOrderException(
                    "Order creation failed due to unexpected error: " + e.getMessage(),
                    "ORDER_CREATION_ERROR",
                    null,
                    e
                );
            }
        }
    }
    
    /**
     * Checks if an order with the given cartId already exists (idempotency).
     */
    private void checkIdempotency(String cartId) {
        Optional<DomainOrderAggregate> existingOrder = orderRepository.findByCartId(cartId);
        if (existingOrder.isPresent()) {
            log.warn("Attempted to create duplicate order for cartId: {}", cartId);
            throw new DomainOrderException(
                "Order already exists for cartId: " + cartId,
                "DUPLICATE_ORDER",
                existingOrder.get().getOrder().getOrderId().toString()
            );
        }
    }
    
    /**
     * Persists the order aggregate to the database.
     */
    private DomainOrderAggregate persistOrder(DomainOrderAggregate orderAggregate) {
        try {
            return orderRepository.save(orderAggregate);
        } catch (Exception e) {
            log.error("Failed to persist order aggregate", e);
            throw new DomainOrderException(
                "Failed to persist order: " + e.getMessage(),
                "PERSISTENCE_ERROR",
                orderAggregate.getOrder().getOrderId().toString(),
                e
            );
        }
    }
    
    /**
     * Publishes a success event after order creation.
     */
    private void publishSuccessEvent(DomainOrderAggregate orderAggregate, String correlationId) {
        try {
            DomainOrderCreatedEvent event = DomainOrderCreatedEvent.builder()
                .orderId(orderAggregate.getOrderId())
                .orderNumber(orderAggregate.getOrder().getOrderNumber())
                .customerId(orderAggregate.getCustomerId())
                .totalAmount(orderAggregate.getTotalMoney())
                .itemCount(orderAggregate.getItemCount())
                .occurredOn(Instant.now())
                .correlationId(correlationId)
                .build();
            
            eventPublisher.publishOrderCreated(event);
            log.debug("Published order created event for orderId: {}", orderAggregate.getOrder().getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to publish order created event for orderId: {}", 
                orderAggregate.getOrder().getOrderId(), e);
            // Don't throw here as the order was successfully created
        }
    }
    
    /**
     * Publishes a failure event when order creation fails.
     */
    private void publishFailureEvent(String cartId, String customerId, Exception exception) {
        try {
            SharedErrorTypeEnum errorType = determineErrorType(exception);
            
            DomainOrderCreationFailedEvent event = DomainOrderCreationFailedEvent.builder()
                .cartId(cartId)
                .customerId(customerId)
                .errorType(errorType)
                .errorMessage(exception.getMessage())
                .failureReason(exception.getClass().getSimpleName())
                .occurredOn(Instant.now())
                .correlationId(cartId)
                .build();
            
            eventPublisher.publishOrderCreationFailed(event);
            log.debug("Published order creation failed event for cartId: {}", cartId);
            
        } catch (Exception e) {
            log.error("Failed to publish order creation failed event for cartId: {}", cartId, e);
        }
    }
    
    /**
     * Determines the appropriate error type based on the exception.
     */
    private SharedErrorTypeEnum determineErrorType(Exception exception) {
        if (exception instanceof DomainValidationException) {
            return SharedErrorTypeEnum.VALIDATION_ERROR;
        } else if (exception instanceof DomainOrderException) {
            DomainOrderException orderException = (DomainOrderException) exception;
            if ("DUPLICATE_ORDER".equals(orderException.getErrorCode())) {
                return SharedErrorTypeEnum.VALIDATION_ERROR;
            } else if ("PERSISTENCE_ERROR".equals(orderException.getErrorCode())) {
                return SharedErrorTypeEnum.PERSISTENCE_ERROR;
            }
        }
        return SharedErrorTypeEnum.EXTERNAL_SERVICE_ERROR;
    }
}