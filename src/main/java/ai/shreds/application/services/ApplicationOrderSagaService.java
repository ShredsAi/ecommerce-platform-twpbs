package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationCancellationResultDTO;
import ai.shreds.application.dtos.ApplicationOrderCreatedDTO;
import ai.shreds.application.dtos.ApplicationPaymentWebhookDTO;
import ai.shreds.application.dtos.ApplicationPaymentWebhookResultDTO;
import ai.shreds.application.dtos.ApplicationSagaStartResultDTO;
import ai.shreds.application.dtos.ApplicationShippingUpdateDTO;
import ai.shreds.application.dtos.ApplicationShippingUpdateResultDTO;
import ai.shreds.application.dtos.ApplicationTimeoutResultDTO;
import ai.shreds.application.exceptions.ApplicationSagaException;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.domain.ports.DomainInputPortOrderStateTransition;
import ai.shreds.domain.ports.DomainInputPortSagaCoordination;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortNotificationService;
import ai.shreds.domain.ports.DomainOutputPortOrderEventRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.ports.DomainOutputPortSagaStateRepository;
import ai.shreds.domain.ports.DomainOutputPortShippingService;
import ai.shreds.shared.dtos.PaymentResult;
import ai.shreds.shared.dtos.SharedTimeoutDetailDTO;
import ai.shreds.shared.enums.SharedEventTypeEnum;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import ai.shreds.shared.enums.SharedSagaStepEnum;
import ai.shreds.shared.enums.SharedSagaStatusEnum;
import ai.shreds.shared.enums.SharedShippingStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Application service orchestrating the order fulfillment saga.
 * Responsible for coordinating the entire fulfillment process from order creation to delivery,
 * handling payment authorizations/captures, shipping arrangements, and timeouts.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApplicationOrderSagaService implements 
        ai.shreds.application.ports.ApplicationInputPortStartSaga,
        ai.shreds.application.ports.ApplicationInputPortHandleCancellation,
        ai.shreds.application.ports.ApplicationInputPortHandlePaymentWebhook,
        ai.shreds.application.ports.ApplicationInputPortHandleShippingUpdate,
        ai.shreds.application.ports.ApplicationInputPortHandleTimeout {

    // Repositories and Domain Services
    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainOutputPortSagaStateRepository sagaStateRepository;
    private final DomainOutputPortOrderEventRepository orderEventRepository;
    private final DomainInputPortOrderStateTransition orderStateMachineService;
    private final DomainInputPortSagaCoordination sagaOrchestrationService;
    
    // External Service Adapters
    private final DomainOutputPortPaymentService paymentService;
    private final DomainOutputPortShippingService shippingService;
    private final DomainOutputPortEventPublisher eventPublisher;
    private final DomainOutputPortNotificationService notificationService;
    
    // Application Support Services
    private final ApplicationSagaTimeoutService timeoutService;
    private final ApplicationCompensationService compensationService;
    private final RedisTemplate<String, String> redisTemplate;

    // Constants
    private static final String IDEMPOTENCY_KEY_PREFIX = "saga:idempotent:";
    private static final String PAYMENT_IDEMPOTENCY_PREFIX = "payment:";
    private static final String SHIPPING_IDEMPOTENCY_PREFIX = "shipping:";
    private static final String EVENT_IDEMPOTENCY_PREFIX = "event:";
    private static final int MAX_RETRIES = 3;
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);
    private static final Duration LONG_IDEMPOTENCY_TTL = Duration.ofHours(24);

    /**
     * Initiates the order fulfillment saga for a newly created order.
     * Creates saga state, persists initial events, and begins payment authorization.
     *
     * @param orderCreated Order creation details
     * @return Saga initialization result with saga ID and status
     * @throws ApplicationSagaException if saga initialization fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Retryable(maxAttempts = 2, value = org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ApplicationSagaStartResultDTO startSaga(ApplicationOrderCreatedDTO orderCreated) {
        try {
            log.info("Starting saga for order: {}", orderCreated.getOrderId());
            
            // Check idempotency to prevent duplicate saga creation
            String idempotencyKey = EVENT_IDEMPOTENCY_PREFIX + orderCreated.getOrderId();
            if (!checkIdempotency(idempotencyKey)) {
                log.warn("Duplicate saga start request for order: {}", orderCreated.getOrderId());
                
                // If saga exists, return current state
                Optional<DomainSagaStateEntity> existingSaga = sagaStateRepository.findByOrderId(orderCreated.getOrderId());
                if (existingSaga.isPresent()) {
                    DomainSagaStateEntity saga = existingSaga.get();
                    return new ApplicationSagaStartResultDTO(
                        saga.getSagaId(),
                        saga.getOrderId(),
                        saga.getStatus().name(),
                        saga.getCurrentStep().name(),
                        saga.getCreatedAt()
                    );
                }
            }
            
            // Convert and persist the order
            DomainOrderEntity order = orderCreated.toDomainOrder();
            orderRepository.save(order);
            
            // Create and persist order creation event
            DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                SharedEventTypeEnum.ORDER_CREATED,
                "Order saga started"
            );
            orderEventRepository.save(orderEvent);
            
            // Initialize saga state through domain service
            DomainSagaStateEntity sagaState = sagaOrchestrationService.initiateSaga(order);
            
            // Publish event to notify other systems
            eventPublisher.publishFulfillmentEvent(
                sagaState.getSagaId(),
                order.getOrderId(),
                sagaState.getCurrentStep().name(),
                sagaState.getStatus().name()
            );
            
            // Begin payment authorization process
            initiatePaymentAuthorization(order, sagaState);
            
            log.info("Saga started successfully for order: {}, sagaId: {}", 
                orderCreated.getOrderId(), sagaState.getSagaId());
                
            return new ApplicationSagaStartResultDTO(
                sagaState.getSagaId(),
                order.getOrderId(),
                sagaState.getStatus().name(),
                sagaState.getCurrentStep().name(),
                sagaState.getCreatedAt()
            );
        } catch (Exception ex) {
            log.error("Failed to start saga for order: {}", orderCreated.getOrderId(), ex);
            throw new ApplicationSagaException("Failed to start saga: " + ex.getMessage(), ex);
        }
    }

    /**
     * Handles order cancellation requests, executing compensation actions as needed.
     * Validates if cancellation is allowed in current state and executes compensation chain.
     *
     * @param cancellation Order cancellation details
     * @return Cancellation result with status and refund information
     * @throws ApplicationSagaException if cancellation process fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ApplicationCancellationResultDTO handleCancellation(ai.shreds.application.dtos.ApplicationOrderCancelledDTO cancellation) {
        try {
            log.info("Handling cancellation for order: {}", cancellation.getOrderId());
            
            // Check idempotency for cancellation requests
            String idempotencyKey = EVENT_IDEMPOTENCY_PREFIX + "cancel:" + cancellation.getOrderId();
            if (!checkIdempotency(idempotencyKey)) {
                log.warn("Duplicate cancellation request for order: {}", cancellation.getOrderId());
                return new ApplicationCancellationResultDTO(
                    cancellation.getOrderId(),
                    "ALREADY_CANCELLED",
                    "NO_REFUND",
                    "Cancellation request already processed",
                    Instant.now()
                );
            }
            
            // Fetch order to be cancelled
            DomainOrderEntity order = orderRepository.findById(cancellation.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException("Order not found for cancellation: " + 
                    cancellation.getOrderId(), null));
            
            // Validate if order can be cancelled in current state
            if (!orderStateMachineService.validateTransition(order.getOrderStatus(), SharedOrderStatusEnum.CANCELLED)) {
                log.warn("Order cannot be cancelled in current state: {}", order.getOrderStatus());
                return new ApplicationCancellationResultDTO(
                    order.getOrderId(),
                    "CANCELLATION_NOT_ALLOWED",
                    "NO_REFUND",
                    "Order cannot be cancelled in current state: " + order.getOrderStatus(),
                    Instant.now()
                );
            }
            
            // Record previous status before state change
            SharedOrderStatusEnum previous = order.getOrderStatus();
            
            // Transition order to cancelled state
            order.transitionTo(SharedOrderStatusEnum.CANCELLED);
            orderRepository.save(order);
            
            // Execute compensation if necessary
            Optional<DomainSagaStateEntity> sagaState = sagaStateRepository.findByOrderId(cancellation.getOrderId());
            if (sagaState.isPresent()) {
                compensationService.executeCompensationChain(order, sagaState.get().getCurrentStep().name());
                
                // Update saga state to reflect cancellation
                sagaState.get().setStatus(SharedSagaStatusEnum.FAILED);
                sagaStateRepository.save(sagaState.get());
            }
            
            // Publish order status change event
            eventPublisher.publishOrderStatusChanged(
                order.getOrderId(),
                previous,
                order.getOrderStatus()
            );
            
            // Notify customer of cancellation
            notifyCustomerOfCancellation(order, cancellation.getCancellationReason());
            
            // Create order cancellation event
            DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                SharedEventTypeEnum.ORDER_CANCELLED,
                "Order cancelled: " + cancellation.getCancellationReason()
            );
            orderEventRepository.save(orderEvent);
            
            log.info("Cancellation processed successfully for order: {}", cancellation.getOrderId());
            return new ApplicationCancellationResultDTO(
                order.getOrderId(),
                "CANCELLED",
                cancellation.getRefundRequired() ? "REFUND_INITIATED" : "NO_REFUND",
                "Cancellation processed successfully",
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error handling cancellation for order: {}", cancellation.getOrderId(), ex);
            throw new ApplicationSagaException(
                "Error handling cancellation: " + cancellation.getOrderId(), 
                ex
            );
        }
    }

    /**
     * Processes payment webhooks from payment provider, updating saga state and
     * progressing to subsequent steps based on payment status.
     *
     * @param payment Payment webhook data
     * @return Payment webhook processing result
     * @throws ApplicationSagaException if payment processing fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ApplicationPaymentWebhookResultDTO handlePaymentWebhook(ApplicationPaymentWebhookDTO payment) {
        try {
            log.info("Handling payment webhook for order: {}, transaction: {}, status: {}", 
                payment.getOrderId(), payment.getTransactionId(), payment.getStatus());
            
            // Check idempotency for this specific payment transaction
            if (!checkIdempotency(PAYMENT_IDEMPOTENCY_PREFIX + payment.getTransactionId())) {
                log.info("Duplicate payment webhook detected for transaction: {}", payment.getTransactionId());
                return new ApplicationPaymentWebhookResultDTO(
                    payment.getStatus(),
                    payment.getOrderId(),
                    "ALREADY_PROCESSED",
                    Instant.now()
                );
            }
            
            // Retrieve saga state - must exist for valid payment webhook
            DomainSagaStateEntity sagaState = sagaStateRepository.findByOrderId(payment.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException(
                    "Saga not found for order: " + payment.getOrderId(), null));
            
            // Process payment based on status
            SharedPaymentStatusEnum paymentStatus = SharedPaymentStatusEnum.valueOf(payment.getStatus());
            DomainOrderEntity order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException(
                    "Order not found for payment webhook: " + payment.getOrderId(), null));
                
            // Create payment event
            DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                mapPaymentStatusToEventType(paymentStatus),
                "Payment webhook processed: " + payment.getStatus() + ", transactionId: " + payment.getTransactionId()
            );
            orderEventRepository.save(orderEvent);
                
            // Route to appropriate handler based on payment status
            switch (paymentStatus) {
                case AUTHORIZED:
                    return handlePaymentAuthorized(payment, sagaState, order);
                    
                case CAPTURED:
                    return handlePaymentCaptured(payment, sagaState, order);
                    
                case FAILED:
                    return handlePaymentFailed(payment, sagaState, order);
                    
                case REFUNDED:
                    return handlePaymentRefunded(payment, sagaState, order);
                    
                default:
                    log.warn("Unhandled payment status: {} for order {}", payment.getStatus(), payment.getOrderId());
                    return new ApplicationPaymentWebhookResultDTO(
                        payment.getStatus(),
                        payment.getOrderId(),
                        "UNRECOGNIZED_STATUS",
                        Instant.now()
                    );
            }
        } catch (Exception ex) {
            log.error("Error handling payment webhook for order: {}", payment.getOrderId(), ex);
            throw new ApplicationSagaException(
                "Error handling payment webhook for order: " + payment.getOrderId(), 
                ex
            );
        }
    }

    /**
     * Processes shipping updates from shipping provider, updating order status
     * and triggering subsequent fulfillment steps.
     *
     * @param update Shipping update details
     * @return Shipping update processing result
     * @throws ApplicationSagaException if shipping update processing fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ApplicationShippingUpdateResultDTO handleShippingUpdate(ApplicationShippingUpdateDTO update) {
        try {
            log.info("Handling shipping update for order: {}, status: {}, tracking: {}", 
                update.getOrderId(), update.getStatus(), update.getTrackingNumber());
            
            // Check idempotency for this specific shipping update
            String idempotencyKey = SHIPPING_IDEMPOTENCY_PREFIX + update.getTrackingNumber() + ":" + update.getStatus();
            if (!checkIdempotency(idempotencyKey)) {
                log.info("Duplicate shipping update detected for tracking: {}, status: {}", 
                    update.getTrackingNumber(), update.getStatus());
                return new ApplicationShippingUpdateResultDTO(
                    update.getOrderId(),
                    update.getStatus(),
                    true,
                    Instant.now()
                );
            }
            
            // Retrieve saga state and order
            DomainSagaStateEntity sagaState = sagaStateRepository.findByOrderId(update.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException(
                    "Saga not found for order: " + update.getOrderId(), null));
            
            DomainOrderEntity order = orderRepository.findById(update.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException(
                    "Order not found: " + update.getOrderId(), null));
            
            // Map shipping status to corresponding order status
            SharedShippingStatusEnum shippingStatus = SharedShippingStatusEnum.valueOf(update.getStatus());
            SharedOrderStatusEnum newOrderStatus = mapShippingToOrderStatus(shippingStatus);
            
            // Create shipping event
            DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                mapShippingStatusToEventType(shippingStatus),
                "Shipping update: " + update.getStatus() + ", tracking: " + update.getTrackingNumber()
            );
            orderEventRepository.save(orderEvent);
            
            // Update order status if valid transition
            SharedOrderStatusEnum previousStatus = order.getOrderStatus();
            boolean statusUpdated = false;
            
            if (orderStateMachineService.validateTransition(previousStatus, newOrderStatus)) {
                order.transitionTo(newOrderStatus);
                orderRepository.save(order);
                statusUpdated = true;
                
                // Publish order status change event
                eventPublisher.publishOrderStatusChanged(
                    order.getOrderId(),
                    previousStatus,
                    newOrderStatus
                );
            } else {
                log.warn("Invalid order status transition attempted: {} -> {} for order {}",
                    previousStatus, newOrderStatus, order.getOrderId());
            }
            
            // Notify customer of shipping update
            notificationService.notifyCustomer(
                order.getCustomerId(),
                "SHIPPING_UPDATE",
                buildShippingNotificationData(order, update)
            );
            
            // Update saga state based on shipping status
            updateSagaStateForShippingUpdate(sagaState, shippingStatus);
            
            // If order is delivered, trigger payment capture if not already done
            if (shippingStatus == SharedShippingStatusEnum.DELIVERED) {
                capturePaymentIfNeeded(order);
            }
            
            log.info("Shipping update processed successfully for order: {}, status updated: {}",
                update.getOrderId(), statusUpdated);
                
            return new ApplicationShippingUpdateResultDTO(
                update.getOrderId(),
                update.getStatus(),
                true,
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error handling shipping update for order: {}", update.getOrderId(), ex);
            throw new ApplicationSagaException(
                "Error handling shipping update for order: " + update.getOrderId(), 
                ex
            );
        }
    }

    /**
     * Detects and processes timed-out sagas, applying retry logic or marking as failed
     * based on configured policies.
     *
     * @return Summary of timeout processing results
     * @throws ApplicationSagaException if timeout handling fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ApplicationTimeoutResultDTO handleTimeouts() {
        try {
            log.info("Processing saga timeouts");
            long startTime = System.currentTimeMillis();
            
            // Define cutoff time for detecting timeouts (30 minutes in the past)
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutService.getTimeoutThresholdMinutes());
            
            // Find all sagas that have timed out
            List<DomainSagaStateEntity> timedOut = timeoutService.detectTimeouts(cutoff);
            log.info("Found {} timed out sagas", timedOut.size());
            
            // Process each timed out saga
            List<SharedTimeoutDetailDTO> details = timedOut.stream()
                .map(this::processTimeoutSaga)
                .collect(Collectors.toList());
            
            // Calculate statistics
            int processed = details.size();
            int failed = (int) details.stream().filter(d -> !d.isSuccess()).count();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Processed {} timeouts, {} failed, execution time: {} ms", 
                processed, failed, executionTime);
                
            return new ApplicationTimeoutResultDTO(processed, failed, details, executionTime);
        } catch (Exception ex) {
            log.error("Error handling saga timeouts", ex);
            throw new ApplicationSagaException("Error handling saga timeouts: " + ex.getMessage(), ex);
        }
    }

    /**
     * Checks if an event has already been processed using Redis-based idempotency store
     * with appropriate TTL.
     *
     * @param eventId Unique identifier for the event
     * @return true if this is the first occurrence of the event, false if already processed
     */
    private boolean checkIdempotency(String eventId) {
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + eventId;
            Boolean isFirstOccurrence = redisTemplate.opsForValue().setIfAbsent(key, "processed", IDEMPOTENCY_TTL);
            return Boolean.TRUE.equals(isFirstOccurrence);
        } catch (Exception e) {
            log.warn("Redis idempotency check failed, allowing processing: {}", e.getMessage());
            return true; // Allow processing if Redis is down or unreachable
        }
    }

    /**
     * Updates a saga state with new step and status information.
     * Records activity timestamp and persists changes.
     *
     * @param sagaId ID of the saga to update
     * @param step Current step being processed or completed
     * @param status Current status of the saga
     */
    private void updateSagaState(UUID sagaId, String step, String status) {
        try {
            sagaStateRepository.findById(sagaId).ifPresent(saga -> {
                saga.updateStep(SharedSagaStepEnum.valueOf(step));
                saga.setStatus(SharedSagaStatusEnum.valueOf(status));
                saga.setLastActivity(Instant.now());
                sagaStateRepository.save(saga);
                
                log.debug("Updated saga state: id={}, step={}, status={}", 
                    sagaId, step, status);
            });
        } catch (Exception ex) {
            log.error("Failed to update saga state: {}", sagaId, ex);
        }
    }

    /**
     * Determines if retry should be attempted based on retry count and configured limits.
     *
     * @param sagaId ID of the saga being retried
     * @param retryCount Current retry count
     * @return true if retry should be attempted, false if max retries exceeded
     */
    private boolean applyRetryLogic(UUID sagaId, int retryCount) {
        boolean shouldRetry = retryCount < MAX_RETRIES;
        log.debug("Retry decision for saga {}: retry count={}, should retry={}", 
            sagaId, retryCount, shouldRetry);
        return shouldRetry;
    }

    /**
     * Initiates payment authorization process for an order.
     * Updates saga state based on success or failure.
     *
     * @param order Order to authorize payment for
     * @param sagaState Current saga state
     */
    private void initiatePaymentAuthorization(DomainOrderEntity order, DomainSagaStateEntity sagaState) {
        try {
            log.info("Initiating payment authorization for order: {}", order.getOrderId());
            
            // Request payment authorization from payment service
            PaymentResult paymentResult = paymentService.authorize(order);
            
            if (Boolean.TRUE.equals(paymentResult.getSuccess())) {
                log.info("Payment authorization successful for order: {}, transactionId: {}", 
                    order.getOrderId(), paymentResult.getTransactionId());
                    
                // Update saga state to reflect successful authorization
                sagaState.updateStep(SharedSagaStepEnum.PAYMENT_AUTHORIZATION);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                // Create payment authorized event
                DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                    order.getOrderId(),
                    SharedEventTypeEnum.PAYMENT_AUTHORIZED,
                    "Payment authorized: " + paymentResult.getTransactionId()
                );
                orderEventRepository.save(orderEvent);
            } else {
                log.error("Payment authorization failed for order: {}, error: {}", 
                    order.getOrderId(), paymentResult.getErrorMessage());
                    
                // Update saga state to initiate compensation
                sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_PAYMENT);
                sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                // Create payment failure event
                DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                    order.getOrderId(),
                    SharedEventTypeEnum.PAYMENT_FAILED,
                    "Payment authorization failed: " + paymentResult.getErrorMessage()
                );
                orderEventRepository.save(orderEvent);
            }
        } catch (Exception e) {
            log.error("Exception during payment authorization for order: {}", order.getOrderId(), e);
            
            // Update saga state to handle failure
            sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_PAYMENT);
            sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
        }
    }

    /**
     * Handles payment authorized status from webhook.
     * Advances saga to next step (shipping arrangement).
     */
    private ApplicationPaymentWebhookResultDTO handlePaymentAuthorized(
            ApplicationPaymentWebhookDTO payment, 
            DomainSagaStateEntity sagaState, 
            DomainOrderEntity order) {
            
        try {
            log.info("Processing AUTHORIZED payment for order: {}", payment.getOrderId());
            
            // Update saga state to move to shipping arrangement
            sagaState.updateStep(SharedSagaStepEnum.SHIPPING_ARRANGEMENT);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            // Arrange shipping for the order
            processShippingArrangement(sagaState);
            
            return new ApplicationPaymentWebhookResultDTO(
                payment.getStatus(),
                payment.getOrderId(),
                sagaState.getCurrentStep().name(),
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error processing authorized payment for order: {}", payment.getOrderId(), ex);
            sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_PAYMENT);
            sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
            sagaStateRepository.save(sagaState);
            
            throw ex;
        }
    }

    /**
     * Handles payment captured status from webhook.
     * Updates order status to PAID and potentially completes the saga.
     */
    private ApplicationPaymentWebhookResultDTO handlePaymentCaptured(
            ApplicationPaymentWebhookDTO payment, 
            DomainSagaStateEntity sagaState, 
            DomainOrderEntity order) {
            
        try {
            log.info("Processing CAPTURED payment for order: {}", payment.getOrderId());
            
            // Update saga state for payment capture step
            sagaState.updateStep(SharedSagaStepEnum.PAYMENT_CAPTURE);
            sagaState.setLastActivity(Instant.now());
            
            // Update order status to PAID if valid transition
            SharedOrderStatusEnum previousStatus = order.getOrderStatus();
            if (orderStateMachineService.validateTransition(previousStatus, SharedOrderStatusEnum.PAID)) {
                order.transitionTo(SharedOrderStatusEnum.PAID);
                orderRepository.save(order);
                
                // Publish order status change
                eventPublisher.publishOrderStatusChanged(
                    order.getOrderId(),
                    previousStatus,
                    SharedOrderStatusEnum.PAID
                );
                
                // Notify customer
                notificationService.notifyCustomer(
                    order.getCustomerId(),
                    "PAYMENT_CAPTURED",
                    Map.of(
                        "orderId", order.getOrderId().toString(),
                        "amount", payment.getAmount().getValue().toString(),
                        "currency", payment.getAmount().getCurrency(),
                        "transactionId", payment.getTransactionId()
                    )
                );
            }
            
            // Mark saga as completed if appropriate
            if (isOrderFulfillmentComplete(order)) {
                sagaState.setStatus(SharedSagaStatusEnum.COMPLETED);
                log.info("Saga completed for order: {}", payment.getOrderId());
            }
            
            sagaStateRepository.save(sagaState);
            
            return new ApplicationPaymentWebhookResultDTO(
                payment.getStatus(),
                payment.getOrderId(),
                sagaState.getCurrentStep().name(),
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error processing captured payment for order: {}", payment.getOrderId(), ex);
            throw ex;
        }
    }

    /**
     * Handles payment failed status from webhook.
     * Initiates compensation actions.
     */
    private ApplicationPaymentWebhookResultDTO handlePaymentFailed(
            ApplicationPaymentWebhookDTO payment, 
            DomainSagaStateEntity sagaState, 
            DomainOrderEntity order) {
            
        try {
            log.info("Processing FAILED payment for order: {}", payment.getOrderId());
            
            // Update saga state to initiate compensation
            sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_PAYMENT);
            sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            // Execute compensation chain
            compensationService.executeCompensationChain(order, sagaState.getCurrentStep().name());
            
            // Update saga state to reflect completion of compensation
            sagaState.setStatus(SharedSagaStatusEnum.FAILED);
            sagaStateRepository.save(sagaState);
            
            // Notify customer
            notificationService.notifyCustomer(
                order.getCustomerId(),
                "PAYMENT_FAILED",
                Map.of(
                    "orderId", order.getOrderId().toString(),
                    "message", "Your payment could not be processed. Please contact customer support."
                )
            );
            
            return new ApplicationPaymentWebhookResultDTO(
                payment.getStatus(),
                payment.getOrderId(),
                "PAYMENT_FAILED_COMPENSATED",
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error handling payment failure for order: {}", payment.getOrderId(), ex);
            sagaState.setStatus(SharedSagaStatusEnum.FAILED);
            sagaStateRepository.save(sagaState);
            
            throw ex;
        }
    }

    /**
     * Handles payment refunded status from webhook.
     * Updates order status and completes compensation if in progress.
     */
    private ApplicationPaymentWebhookResultDTO handlePaymentRefunded(
            ApplicationPaymentWebhookDTO payment, 
            DomainSagaStateEntity sagaState, 
            DomainOrderEntity order) {
            
        try {
            log.info("Processing REFUNDED payment for order: {}", payment.getOrderId());
            
            // Update order status to cancelled if appropriate
            SharedOrderStatusEnum previousStatus = order.getOrderStatus();
            if (order.getOrderStatus() != SharedOrderStatusEnum.CANCELLED &&
                orderStateMachineService.validateTransition(previousStatus, SharedOrderStatusEnum.CANCELLED)) {
                
                order.transitionTo(SharedOrderStatusEnum.CANCELLED);
                orderRepository.save(order);
                
                // Publish order status change
                eventPublisher.publishOrderStatusChanged(
                    order.getOrderId(),
                    previousStatus,
                    SharedOrderStatusEnum.CANCELLED
                );
            }
            
            // Update saga state
            if (sagaState.getStatus() == SharedSagaStatusEnum.COMPENSATING) {
                sagaState.setStatus(SharedSagaStatusEnum.FAILED);
                log.info("Compensation completed for order: {}", payment.getOrderId());
            }
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            // Notify customer
            notificationService.notifyCustomer(
                order.getCustomerId(),
                "PAYMENT_REFUNDED",
                Map.of(
                    "orderId", order.getOrderId().toString(),
                    "amount", payment.getAmount().getValue().toString(),
                    "currency", payment.getAmount().getCurrency(),
                    "transactionId", payment.getTransactionId()
                )
            );
            
            return new ApplicationPaymentWebhookResultDTO(
                payment.getStatus(),
                payment.getOrderId(),
                "REFUND_COMPLETED",
                Instant.now()
            );
        } catch (Exception ex) {
            log.error("Error processing refunded payment for order: {}", payment.getOrderId(), ex);
            throw ex;
        }
    }

    /**
     * Arranges shipping for an order and updates saga state based on outcome.
     *
     * @param sagaState Current saga state
     */
    private void processShippingArrangement(DomainSagaStateEntity sagaState) {
        try {
            log.info("Processing shipping arrangement for order: {}", sagaState.getOrderId());
            
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException("Order not found", null));
            
            // Request shipping arrangement from shipping service
            var shipmentResult = shippingService.createShipment(order);
            
            if (Boolean.TRUE.equals(shipmentResult.getSuccess())) {
                log.info("Shipping arranged successfully for order: {}, tracking: {}", 
                    sagaState.getOrderId(), shipmentResult.getTrackingNumber());
                
                // Update order status to PROCESSING
                SharedOrderStatusEnum previousStatus = order.getOrderStatus();
                if (orderStateMachineService.validateTransition(previousStatus, SharedOrderStatusEnum.PROCESSING)) {
                    order.transitionTo(SharedOrderStatusEnum.PROCESSING);
                    orderRepository.save(order);
                    
                    // Publish order status change
                    eventPublisher.publishOrderStatusChanged(
                        order.getOrderId(),
                        previousStatus,
                        SharedOrderStatusEnum.PROCESSING
                    );
                }
                
                // Create shipping arranged event
                DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                    order.getOrderId(),
                    SharedEventTypeEnum.SHIPPING_ARRANGED,
                    "Shipping arranged: " + shipmentResult.getTrackingNumber() + 
                    ", carrier: " + shipmentResult.getCarrier()
                );
                orderEventRepository.save(orderEvent);
                
                // Update saga state
                sagaState.updateStep(SharedSagaStepEnum.SHIPPING_ARRANGEMENT);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                // Subscribe for shipping updates
                shippingService.subscribeToUpdates(order.getOrderId());
                
                // Notify customer
                notificationService.notifyCustomer(
                    order.getCustomerId(),
                    "SHIPPING_ARRANGED",
                    Map.of(
                        "orderId", order.getOrderId().toString(),
                        "trackingNumber", shipmentResult.getTrackingNumber(),
                        "carrier", shipmentResult.getCarrier(),
                        "estimatedDelivery", shipmentResult.getEstimatedDeliveryDate().toString()
                    )
                );
            } else {
                log.error("Shipping arrangement failed for order: {}, error: {}", 
                    sagaState.getOrderId(), shipmentResult.getErrorMessage());
                
                // Update saga state to start compensation
                sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_SHIPPING);
                sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                // Create shipping failure event
                DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                    order.getOrderId(),
                    SharedEventTypeEnum.SHIPPING_FAILED,
                    "Shipping arrangement failed: " + shipmentResult.getErrorMessage()
                );
                orderEventRepository.save(orderEvent);
                
                // Execute compensation
                compensationService.executeCompensationChain(order, sagaState.getCurrentStep().name());
            }
        } catch (Exception e) {
            log.error("Exception during shipping arrangement for order: {}", sagaState.getOrderId(), e);
            
            // Update saga state to handle failure
            sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_SHIPPING);
            sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            throw new ApplicationSagaException(
                sagaState.getSagaId(),
                sagaState.getCurrentStep().name(),
                "Shipping arrangement failed", 
                e
            );
        }
    }

    /**
     * Handles processing of a payment capture step after shipping completion.
     *
     * @param sagaState Current saga state
     */
    private void processPaymentCapture(DomainSagaStateEntity sagaState) {
        try {
            log.info("Processing payment capture for order: {}", sagaState.getOrderId());
            
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException("Order not found", null));
            
            // Update order status to PAID if valid transition
            SharedOrderStatusEnum previousStatus = order.getOrderStatus();
            if (orderStateMachineService.validateTransition(previousStatus, SharedOrderStatusEnum.PAID)) {
                order.transitionTo(SharedOrderStatusEnum.PAID);
                orderRepository.save(order);
                
                // Publish order status change
                eventPublisher.publishOrderStatusChanged(
                    order.getOrderId(),
                    previousStatus,
                    SharedOrderStatusEnum.PAID
                );
            }
            
            // Update saga state to mark completion
            if (isOrderFulfillmentComplete(order)) {
                sagaState.setStatus(SharedSagaStatusEnum.COMPLETED);
                log.info("Saga completed for order: {}", order.getOrderId());
            }
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            log.error("Payment capture processing failed for saga: {}", sagaState.getSagaId(), e);
            
            // Update saga state to handle failure
            sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_PAYMENT);
            sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            throw new ApplicationSagaException(
                sagaState.getSagaId(),
                sagaState.getCurrentStep().name(),
                "Payment capture failed", 
                e
            );
        }
    }

    /**
     * Processes a payment failure by executing compensation actions.
     *
     * @param sagaState Current saga state
     */
    private void processPaymentFailure(DomainSagaStateEntity sagaState) {
        try {
            log.info("Processing payment failure for order: {}", sagaState.getOrderId());
            
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException("Order not found", null));
            
            // Execute compensation actions
            compensationService.executeCompensationChain(order, sagaState.getCurrentStep().name());
            
            // Update saga state to mark failure
            sagaState.setStatus(SharedSagaStatusEnum.FAILED);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            // Create payment failure event
            DomainOrderEventEntity orderEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                SharedEventTypeEnum.PAYMENT_FAILED,
                "Payment failed, compensated"
            );
            orderEventRepository.save(orderEvent);
            
            log.info("Payment failure processed and compensated for order: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Error processing payment failure for saga: {}", sagaState.getSagaId(), e);
            
            // Ensure saga is marked as failed even if compensation fails
            sagaState.setStatus(SharedSagaStatusEnum.FAILED);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            throw new ApplicationSagaException(
                sagaState.getSagaId(),
                sagaState.getCurrentStep().name(),
                "Failed to process payment failure", 
                e
            );
        }
    }

    /**
     * Processes a timeout for a saga, applying retry logic or escalation.
     *
     * @param sagaState Timed out saga state
     * @return Details of timeout processing results
     */
    private SharedTimeoutDetailDTO processTimeoutSaga(DomainSagaStateEntity sagaState) {
        try {
            log.info("Processing timeout for saga: {}, retry count: {}, step: {}", 
                sagaState.getSagaId(), sagaState.getRetryCount(), sagaState.getCurrentStep());
            
            // Check if retry should be attempted
            if (applyRetryLogic(sagaState.getSagaId(), sagaState.getRetryCount())) {
                // Increment retry count and set next retry time
                sagaState.incrementRetryCount();
                sagaState.setLastActivity(Instant.now());
                sagaState.setNextRetry(timeoutService.calculateNextRetryTime(sagaState.getRetryCount()).atZone(
                    java.time.ZoneId.systemDefault()).toInstant());
                sagaStateRepository.save(sagaState);
                
                // Retry the current step
                retrySagaStep(sagaState);
                
                return new SharedTimeoutDetailDTO(
                    sagaState.getSagaId(),
                    sagaState.getOrderId(),
                    sagaState.getCurrentStep().name(),
                    "RETRIED",
                    true,
                    null
                );
            } else {
                // Max retries exceeded, mark saga as timed out
                sagaState.setStatus(SharedSagaStatusEnum.TIMED_OUT);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                // Create timeout event
                DomainOrderEventEntity timeoutEvent = new DomainOrderEventEntity(
                    sagaState.getOrderId(),
                    SharedEventTypeEnum.SAGA_TIMEOUT_EXHAUSTED,
                    "Max retries exceeded for step: " + sagaState.getCurrentStep().name()
                );
                orderEventRepository.save(timeoutEvent);
                
                // Alert operations    
                alertOperationsAboutTimeout(sagaState);
                
                return new SharedTimeoutDetailDTO(
                    sagaState.getSagaId(),
                    sagaState.getOrderId(),
                    sagaState.getCurrentStep().name(),
                    "TIMED_OUT",
                    true,
                    "Maximum retry attempts exceeded"
                );
            }
        } catch (Exception e) {
            log.error("Error processing timeout for saga: {}", sagaState.getSagaId(), e);
            
            return new SharedTimeoutDetailDTO(
                sagaState.getSagaId(),
                sagaState.getOrderId(),
                sagaState.getCurrentStep().name(),
                "ERROR",
                false,
                e.getMessage()
            );
        }
    }

    /**
     * Updates saga state based on shipping status update.
     *
     * @param sagaState Current saga state
     * @param shippingStatus Shipping status received
     */
    private void updateSagaStateForShippingUpdate(DomainSagaStateEntity sagaState, SharedShippingStatusEnum shippingStatus) {
        // Update saga state based on shipping status
        switch (shippingStatus) {
            case SHIPPED:
                // Stay in shipping arrangement step but update activity timestamp
                sagaState.setLastActivity(Instant.now());
                break;
                
            case DELIVERED:
                // Move to payment capture step if delivery confirmed
                sagaState.updateStep(SharedSagaStepEnum.PAYMENT_CAPTURE);
                // If all steps complete, mark saga as completed
                sagaState.setStatus(SharedSagaStatusEnum.COMPLETED); 
                sagaState.setLastActivity(Instant.now());
                break;
                
            case FAILED:
                // Move to compensation if shipping failed
                sagaState.updateStep(SharedSagaStepEnum.COMPENSATION_SHIPPING);
                sagaState.setStatus(SharedSagaStatusEnum.COMPENSATING);
                sagaState.setLastActivity(Instant.now());
                break;
                
            default:
                // Just update activity timestamp for other statuses
                sagaState.setLastActivity(Instant.now());
        }
        
        sagaStateRepository.save(sagaState);
    }

    /**
     * Attempts to retry a saga step that has timed out.
     *
     * @param sagaState Saga state to retry
     */
    private void retrySagaStep(DomainSagaStateEntity sagaState) {
        try {
            log.info("Retrying saga step: {} for order: {}, attempt: {}", 
                sagaState.getCurrentStep(), sagaState.getOrderId(), sagaState.getRetryCount());
            
            DomainOrderEntity order = orderRepository.findById(sagaState.getOrderId())
                .orElseThrow(() -> new ApplicationSagaException("Order not found for retry", null));
            
            // Create timeout handled event
            DomainOrderEventEntity timeoutEvent = new DomainOrderEventEntity(
                order.getOrderId(),
                SharedEventTypeEnum.TIMEOUT_HANDLED,
                "Retrying step: " + sagaState.getCurrentStep().name() + ", attempt: " + sagaState.getRetryCount()
            );
            orderEventRepository.save(timeoutEvent);
            
            // Switch based on the current step to retry
            switch (sagaState.getCurrentStep()) {
                case PAYMENT_AUTHORIZATION:
                    initiatePaymentAuthorization(order, sagaState);
                    break;
                    
                case PAYMENT_CAPTURE:
                    processPaymentCapture(sagaState);
                    break;
                    
                case SHIPPING_ARRANGEMENT:
                    processShippingArrangement(sagaState);
                    break;
                    
                case COMPENSATION_PAYMENT:
                case COMPENSATION_SHIPPING:
                case COMPENSATION_INVENTORY:
                    // For compensation steps, retry the compensation chain
                    compensationService.executeCompensationChain(order, sagaState.getCurrentStep().name());
                    break;
                    
                default:
                    log.warn("Unsupported step for retry: {}", sagaState.getCurrentStep());
            }
        } catch (Exception ex) {
            log.error("Failed to retry saga step: {} for order: {}", 
                sagaState.getCurrentStep(), sagaState.getOrderId(), ex);
                
            // Increment retry count despite failure
            sagaState.incrementRetryCount();
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
        }
    }

    /**
     * Maps shipping status to corresponding order status.
     *
     * @param shippingStatus Shipping status to map
     * @return Corresponding order status
     */
    private SharedOrderStatusEnum mapShippingToOrderStatus(SharedShippingStatusEnum shippingStatus) {
        switch (shippingStatus) {
            case SHIPPED:
                return SharedOrderStatusEnum.SHIPPED;
            case DELIVERED:
                return SharedOrderStatusEnum.DELIVERED;
            case FAILED:
                return SharedOrderStatusEnum.CANCELLED;
            case PENDING:
            case ARRANGED:
            default:
                return SharedOrderStatusEnum.PROCESSING;
        }
    }

    /**
     * Maps payment status to corresponding event type.
     *
     * @param paymentStatus Payment status to map
     * @return Corresponding event type
     */
    private SharedEventTypeEnum mapPaymentStatusToEventType(SharedPaymentStatusEnum paymentStatus) {
        switch (paymentStatus) {
            case AUTHORIZED:
                return SharedEventTypeEnum.PAYMENT_AUTHORIZED;
            case CAPTURED:
                return SharedEventTypeEnum.PAYMENT_CAPTURED;
            case FAILED:
                return SharedEventTypeEnum.PAYMENT_FAILED;
            case REFUNDED:
                return SharedEventTypeEnum.ORDER_CANCELLED; // Refund typically happens during cancellation
            default:
                return SharedEventTypeEnum.PAYMENT_INITIATED;
        }
    }

    /**
     * Maps shipping status to corresponding event type.
     *
     * @param shippingStatus Shipping status to map
     * @return Corresponding event type
     */
    private SharedEventTypeEnum mapShippingStatusToEventType(SharedShippingStatusEnum shippingStatus) {
        switch (shippingStatus) {
            case SHIPPED:
                return SharedEventTypeEnum.ORDER_SHIPPED;
            case DELIVERED:
                return SharedEventTypeEnum.ORDER_DELIVERED;
            case FAILED:
                return SharedEventTypeEnum.SHIPPING_FAILED;
            default:
                return SharedEventTypeEnum.SHIPPING_ARRANGED;
        }
    }
    
    /**
     * Checks if order fulfillment is complete based on order status.
     *
     * @param order Order to check
     * @return true if fulfillment is complete, false otherwise
     */
    private boolean isOrderFulfillmentComplete(DomainOrderEntity order) {
        return order.getOrderStatus() == SharedOrderStatusEnum.DELIVERED || 
               order.getOrderStatus() == SharedOrderStatusEnum.COMPLETED ||
               order.getOrderStatus() == SharedOrderStatusEnum.CANCELLED;
    }
    
    /**
     * Captures payment for an order if not already done.
     *
     * @param order Order to capture payment for
     */
    private void capturePaymentIfNeeded(DomainOrderEntity order) {
        // This method would implement payment capture logic after delivery
        // Implementation would check if payment has been captured already, and if not,
        // initiate the capture process
        log.info("Checking if payment capture needed for delivered order: {}", order.getOrderId());
        // Actual implementation details would depend on the payment service interface
    }
    
    /**
     * Notifies customer of order cancellation.
     *
     * @param order Order that was cancelled
     * @param reason Reason for cancellation
     */
    private void notifyCustomerOfCancellation(DomainOrderEntity order, String reason) {
        try {
            notificationService.notifyCustomer(
                order.getCustomerId(),
                "ORDER_CANCELLED",
                Map.of(
                    "orderId", order.getOrderId().toString(),
                    "reason", reason,
                    "cancelledAt", Instant.now().toString()
                )
            );
        } catch (Exception ex) {
            log.error("Failed to send cancellation notification to customer: {}", order.getCustomerId(), ex);
        }
    }
    
    /**
     * Builds shipping notification data map.
     *
     * @param order Order being shipped
     * @param update Shipping update details
     * @return Map of notification data
     */
    private Map<String, Object> buildShippingNotificationData(DomainOrderEntity order, ApplicationShippingUpdateDTO update) {
        return Map.of(
            "orderId", order.getOrderId().toString(),
            "status", update.getStatus(),
            "trackingNumber", update.getTrackingNumber(),
            "carrier", update.getCarrier(),
            "estimatedDelivery", update.getEstimatedDeliveryDate() != null ? 
                update.getEstimatedDeliveryDate().toString() : "Not available"
        );
    }
    
    /**
     * Alerts operations team about a timed-out saga that has exhausted retries.
     *
     * @param sagaState Timed-out saga state
     */
    private void alertOperationsAboutTimeout(DomainSagaStateEntity sagaState) {
        try {
            // This would typically send an alert to an operations monitoring system
            // Implementation would depend on the available alerting mechanisms
            
            log.error("ALERT: Saga {} for order {} has timed out after {} retries in step {}", 
                sagaState.getSagaId(), sagaState.getOrderId(),
                sagaState.getRetryCount(), sagaState.getCurrentStep());
                
            // A real implementation would use an alerting service or send a notification
            // to operations personnel
        } catch (Exception ex) {
            log.error("Failed to send alert about timed out saga: {}", sagaState.getSagaId(), ex);
        }
    }
}