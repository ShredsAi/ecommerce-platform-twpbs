package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentStatusUpdateEntity;
import ai.shreds.domain.events.DomainPaymentFailedEvent;
import ai.shreds.domain.events.DomainPaymentSucceededEvent;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.ports.DomainInputPortPaymentService;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain service implementing payment processing business logic.
 * Handles payment recording, webhook processing, and payment state management.
 */
@Service
public class DomainPaymentService implements DomainInputPortPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(DomainPaymentService.class);
    private final DomainOutputPortPaymentRepository repository;
    private final DomainPaymentFactory factory;
    private final DomainOutputPortEventPublisher eventPublisher;

    public DomainPaymentService(
            DomainOutputPortPaymentRepository repository,
            DomainPaymentFactory factory,
            DomainOutputPortEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.factory = Objects.requireNonNull(factory, "factory cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
    }

    @Override
    public DomainPaymentEntity recordPayment(DomainPaymentIntentEntity intent, DomainProcessorChargeResult result) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(result, "result cannot be null");

        // Validate that intent is in processing state
        if (intent.getStatus() != DomainPaymentStatusEnum.PROCESSING) {
            throw new DomainPaymentException(
                "Cannot record payment for intent not in processing state: " + intent.getStatus(),
                "INVALID_INTENT_STATE"
            );
        }

        // Create payment entity using factory
        DomainPaymentEntity payment = factory.createPayment(intent, result);

        // Persist the payment
        DomainPaymentEntity savedPayment = repository.savePayment(payment);

        // Create status update record
        DomainPaymentStatusUpdateEntity statusUpdate = 
            DomainPaymentStatusUpdateEntity.fromPayment(savedPayment);
        repository.saveStatusUpdate(statusUpdate);

        // Update payment intent status based on payment result
        updateIntentFromPaymentResult(intent, result);
        repository.savePaymentIntent(intent);

        // Publish appropriate domain events
        publishPaymentEvents(savedPayment, intent);

        return savedPayment;
    }

    @Override
    public void handleWebhookStatus(DomainUpdateStatusCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        // Find the payment
        DomainPaymentEntity payment = repository.findPaymentById(command.getPaymentId());
        if (payment == null) {
            throw new DomainPaymentException(
                "Payment not found for webhook update: " + command.getPaymentId(),
                "PAYMENT_NOT_FOUND"
            );
        }

        // Validate status transition
        DomainPaymentStatusEnum currentStatus = payment.getStatus();
        DomainPaymentStatusEnum newStatus = command.getNewStatus();

        if (!isValidWebhookStatusTransition(currentStatus, newStatus)) {
            throw new DomainPaymentException(
                "Invalid webhook status transition from " + currentStatus + " to " + newStatus,
                "INVALID_STATUS_TRANSITION"
            );
        }

        // Always create status update record for reconciliation/audit, even if status doesn't change
        DomainPaymentStatusUpdateEntity statusUpdate = 
            DomainPaymentStatusUpdateEntity.fromPayment(payment);
        repository.saveStatusUpdate(statusUpdate);
        logger.info("Created status update record for payment {} with status {}", payment.getId(), newStatus);

        // Find related payment intent
        DomainPaymentIntentEntity intent = repository.findPaymentIntentById(payment.getPaymentIntentId());
        if (intent == null) {
            logger.warn("Payment intent not found for payment: {}", payment.getId());
            return;
        }

        // Update payment status if needed
        if (currentStatus != newStatus) {
            payment.updateStatus(newStatus);
            repository.savePayment(payment);

            // Update intent status
            intent.updateStatus(newStatus);
            repository.savePaymentIntent(intent);
            
            logger.info("Payment {} status updated from {} to {}", payment.getId(), currentStatus, newStatus);
        }

        // Always publish events based on current/confirmed status for webhook reconciliation
        publishWebhookReconciliationEvents(payment, intent, newStatus);
    }

    /**
     * Processes a failed payment and updates related entities.
     */
    public void processFailedPayment(DomainPaymentEntity payment, String failureReason) {
        Objects.requireNonNull(payment, "payment cannot be null");

        if (payment.isFailed()) {
            return; // Already failed
        }

        // Mark payment as failed
        payment.markFailed();
        repository.savePayment(payment);

        // Update related payment intent
        DomainPaymentIntentEntity intent = repository.findPaymentIntentById(payment.getPaymentIntentId());
        if (intent != null && !intent.isTerminal()) {
            intent.markFailed();
            repository.savePaymentIntent(intent);

            // Publish payment failed event
            DomainPaymentFailedEvent event = new DomainPaymentFailedEvent(
                payment.getId(),
                intent.getId(),
                intent.getOrderId(),
                failureReason != null ? failureReason : "Payment processing failed",
                LocalDateTime.now()
            );
            eventPublisher.publish(event);
        }
    }

    /**
     * Processes a successful payment and updates related entities.
     */
    public void processSuccessfulPayment(DomainPaymentEntity payment) {
        Objects.requireNonNull(payment, "payment cannot be null");

        if (payment.isSuccessful()) {
            return; // Already successful
        }

        // Mark payment as successful
        payment.markSuccessful();
        repository.savePayment(payment);

        // Update related payment intent
        DomainPaymentIntentEntity intent = repository.findPaymentIntentById(payment.getPaymentIntentId());
        if (intent != null && !intent.isTerminal()) {
            intent.markSucceeded();
            repository.savePaymentIntent(intent);

            // Publish payment succeeded event
            DomainPaymentSucceededEvent event = new DomainPaymentSucceededEvent(
                payment.getId(),
                intent.getId(),
                intent.getOrderId(),
                intent.getAmount(),
                LocalDateTime.now()
            );
            eventPublisher.publish(event);
        }
    }

    private void updateIntentFromPaymentResult(DomainPaymentIntentEntity intent, DomainProcessorChargeResult result) {
        switch (result.getStatus()) {
            case SUCCEEDED:
                intent.markSucceeded();
                break;
            case FAILED:
                intent.markFailed();
                break;
            case PROCESSING:
                // Keep current processing status
                break;
            default:
                throw new DomainPaymentException(
                    "Unexpected payment result status: " + result.getStatus(),
                    "UNEXPECTED_RESULT_STATUS"
                );
        }
    }

    private void publishPaymentEvents(DomainPaymentEntity payment, DomainPaymentIntentEntity intent) {
        if (payment.isSuccessful()) {
            DomainPaymentSucceededEvent event = new DomainPaymentSucceededEvent(
                payment.getId(),
                intent.getId(),
                intent.getOrderId(),
                payment.getAmount(),
                LocalDateTime.now()
            );
            eventPublisher.publish(event);
        } else if (payment.isFailed()) {
            DomainPaymentFailedEvent event = new DomainPaymentFailedEvent(
                payment.getId(),
                intent.getId(),
                intent.getOrderId(),
                payment.getFailureReason(),
                LocalDateTime.now()
            );
            eventPublisher.publish(event);
        }
    }

    /**
     * Publishes events for webhook reconciliation.
     * Always publishes events based on the confirmed status, even if no state change occurred.
     * This ensures downstream systems are notified of status confirmations.
     */
    private void publishWebhookReconciliationEvents(DomainPaymentEntity payment, DomainPaymentIntentEntity intent, DomainPaymentStatusEnum confirmedStatus) {
        logger.info("Publishing webhook reconciliation event for payment {} with status {}", payment.getId(), confirmedStatus);
        
        switch (confirmedStatus) {
            case SUCCEEDED:
                DomainPaymentSucceededEvent successEvent = new DomainPaymentSucceededEvent(
                    payment.getId(),
                    intent.getId(),
                    intent.getOrderId(),
                    payment.getAmount(),
                    LocalDateTime.now()
                );
                eventPublisher.publish(successEvent);
                logger.info("Published payment succeeded event for payment ID: {}", payment.getId());
                break;
            case FAILED:
                DomainPaymentFailedEvent failedEvent = new DomainPaymentFailedEvent(
                    payment.getId(),
                    intent.getId(),
                    intent.getOrderId(),
                    payment.getFailureReason() != null ? payment.getFailureReason() : "Payment failed",
                    LocalDateTime.now()
                );
                eventPublisher.publish(failedEvent);
                logger.info("Published payment failed event for payment ID: {}", payment.getId());
                break;
            default:
                logger.warn("No event published for non-terminal status: {}", confirmedStatus);
        }
    }

    private boolean isValidWebhookStatusTransition(DomainPaymentStatusEnum current, DomainPaymentStatusEnum target) {
        // Terminal states cannot be changed
        if (current == DomainPaymentStatusEnum.SUCCEEDED || current == DomainPaymentStatusEnum.FAILED) {
            return current == target; // Only allow same status
        }

        // Non-terminal states can transition to terminal states
        return target == DomainPaymentStatusEnum.SUCCEEDED || target == DomainPaymentStatusEnum.FAILED;
    }
}