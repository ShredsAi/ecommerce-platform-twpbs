package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainConfirmIntentCommand;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.events.DomainPaymentIntentCreatedEvent;
import ai.shreds.domain.events.DomainPaymentProcessingStartedEvent;
import ai.shreds.domain.events.DomainThreeDSecureRequiredEvent;
import ai.shreds.domain.exceptions.DomainInvalidStateException;
import ai.shreds.domain.exceptions.DomainPaymentExpiredException;
import ai.shreds.domain.ports.DomainInputPortPaymentIntent;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain service implementing payment intent business logic.
 * Handles creation, confirmation, and lifecycle management of payment intents.
 */
@Service
public class DomainPaymentIntentService implements DomainInputPortPaymentIntent {
    
    private final DomainOutputPortPaymentRepository repository;
    private final DomainPaymentStateMachine stateMachine;
    private final DomainPaymentIntentFactory factory;
    private final DomainOutputPortEventPublisher eventPublisher;

    public DomainPaymentIntentService(
            DomainOutputPortPaymentRepository repository,
            DomainPaymentStateMachine stateMachine,
            DomainPaymentIntentFactory factory,
            DomainOutputPortEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine cannot be null");
        this.factory = Objects.requireNonNull(factory, "factory cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
    }

    @Override
    public DomainPaymentIntentEntity createIntent(DomainCreateIntentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        // Validate command
        validateCreateCommand(command);
        
        // Create payment intent using factory
        DomainPaymentIntentEntity intent = factory.createIntent(command);
        
        // Persist the intent
        DomainPaymentIntentEntity savedIntent = repository.savePaymentIntent(intent);
        
        // Publish domain event
        DomainPaymentIntentCreatedEvent event = new DomainPaymentIntentCreatedEvent(
                savedIntent.getId(),
                savedIntent.getOrderId(),
                savedIntent.getAmount(),
                LocalDateTime.now()
        );
        eventPublisher.publish(event);
        
        return savedIntent;
    }

    @Override
    public DomainPaymentIntentEntity confirmIntent(DomainConfirmIntentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        // Retrieve the intent
        DomainPaymentIntentEntity intent = repository.findPaymentIntentById(command.getIntentId());
        if (intent == null) {
            throw new IllegalArgumentException("Payment intent not found: " + command.getIntentId());
        }
        
        // Validate client secret
        if (!Objects.equals(intent.getClientSecret(), command.getClientSecret())) {
            throw new IllegalArgumentException("Invalid client secret");
        }
        
        // Check if intent is expired
        if (intent.isExpired()) {
            throw new DomainPaymentExpiredException(intent.getId(), intent.getExpiresAt());
        }
        
        // Validate state transition - we want to transition FROM current state TO PROCESSING
        stateMachine.validateAndTransition(intent.getStatus(), DomainPaymentStatusEnum.PROCESSING);
        
        // Confirm the intent (this sets status to PROCESSING)
        intent.confirm();
        
        // Check if 3D Secure is required
        if (intent.requiresThreeDSecure()) {
            // Publish 3D Secure required event
            DomainThreeDSecureRequiredEvent event = new DomainThreeDSecureRequiredEvent(
                    intent.getId(),
                    "https://3ds.provider.com/challenge/" + intent.getId(), // Placeholder URL
                    LocalDateTime.now()
            );
            eventPublisher.publish(event);
        } else {
            // Publish processing started event
            DomainPaymentProcessingStartedEvent event = new DomainPaymentProcessingStartedEvent(
                    intent.getId(),
                    LocalDateTime.now()
            );
            eventPublisher.publish(event);
        }
        
        // Persist the updated intent
        return repository.savePaymentIntent(intent);
    }

    @Override
    public void expireIntents() {
        List<DomainPaymentIntentEntity> expiredIntents = repository.findExpiredIntents();
        
        for (DomainPaymentIntentEntity intent : expiredIntents) {
            try {
                // Skip if already in terminal state
                if (intent.isTerminal()) {
                    continue;
                }
                
                // Mark as failed due to expiration
                stateMachine.validateAndTransition(intent.getStatus(), DomainPaymentStatusEnum.FAILED);
                intent.markFailed();
                
                // Persist the expired intent
                repository.savePaymentIntent(intent);
                
                // Could publish PaymentFailed event here if needed
                
            } catch (DomainInvalidStateException e) {
                // Log the error but continue processing other intents
                System.err.println("Failed to expire intent " + intent.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Starts processing for a confirmed payment intent.
     */
    public DomainPaymentIntentEntity startProcessing(DomainPaymentIntentEntity intent) {
        Objects.requireNonNull(intent, "intent cannot be null");
        
        // Validate state transition
        stateMachine.validateAndTransition(intent.getStatus(), DomainPaymentStatusEnum.PROCESSING);
        
        // Update status
        intent.startProcessing();
        
        // Publish processing started event
        DomainPaymentProcessingStartedEvent event = new DomainPaymentProcessingStartedEvent(
                intent.getId(),
                LocalDateTime.now()
        );
        eventPublisher.publish(event);
        
        // Persist and return
        return repository.savePaymentIntent(intent);
    }
    
    /**
     * Marks a payment intent as successful.
     */
    public DomainPaymentIntentEntity markSucceeded(DomainPaymentIntentEntity intent) {
        Objects.requireNonNull(intent, "intent cannot be null");
        
        // Validate state transition
        stateMachine.validateAndTransition(intent.getStatus(), DomainPaymentStatusEnum.SUCCEEDED);
        
        // Update status
        intent.markSucceeded();
        
        // Persist and return
        return repository.savePaymentIntent(intent);
    }
    
    /**
     * Marks a payment intent as failed.
     */
    public DomainPaymentIntentEntity markFailed(DomainPaymentIntentEntity intent) {
        Objects.requireNonNull(intent, "intent cannot be null");
        
        // Validate state transition
        stateMachine.validateAndTransition(intent.getStatus(), DomainPaymentStatusEnum.FAILED);
        
        // Update status
        intent.markFailed();
        
        // Persist and return
        return repository.savePaymentIntent(intent);
    }
    
    /**
     * FIXED: Validation now allows null paymentMethodId for OrderPlaced events.
     * Payment method is added later in the flow when customer selects it.
     */
    private void validateCreateCommand(DomainCreateIntentCommand command) {
        if (command.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (command.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (command.getAmount() == null || !command.getAmount().isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        // FIXED: Payment method ID can be null during initial intent creation from OrderPlaced events
        // It will be added later when customer selects a payment method
        // The intent will start in REQUIRES_PAYMENT_METHOD status
    }
}