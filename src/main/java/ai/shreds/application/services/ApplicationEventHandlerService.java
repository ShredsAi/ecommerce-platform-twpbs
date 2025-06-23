package ai.shreds.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.application.dtos.ApplicationOrderPlacedDTO;
import ai.shreds.application.dtos.ApplicationWebhookProcessedDTO;
import ai.shreds.application.exceptions.ApplicationPaymentProcessingException;
import ai.shreds.application.exceptions.ApplicationPaymentNotFoundException;
import ai.shreds.application.ports.ApplicationProcessOrderPlacedInputPort;
import ai.shreds.application.ports.ApplicationProcessWebhookInputPort;
import ai.shreds.application.ports.ApplicationKafkaPublisherOutputPort;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.ports.DomainInputPortPaymentIntent;
import ai.shreds.domain.ports.DomainInputPortPaymentService;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.events.DomainPaymentSucceededEvent;
import ai.shreds.domain.events.DomainPaymentFailedEvent;
import ai.shreds.shared.dtos.SharedPaymentSucceededEvent;
import ai.shreds.shared.dtos.SharedPaymentFailedEvent;

/**
 * Application service for handling events from external sources
 */
@Service
public class ApplicationEventHandlerService implements ApplicationProcessOrderPlacedInputPort, ApplicationProcessWebhookInputPort {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationEventHandlerService.class);
    
    private final DomainInputPortPaymentIntent domainPaymentIntentPort;
    private final DomainInputPortPaymentService domainPaymentServicePort;
    private final DomainOutputPortPaymentRepository paymentRepository;
    private final ApplicationKafkaPublisherOutputPort kafkaPublisher;

    public ApplicationEventHandlerService(
            DomainInputPortPaymentIntent domainPaymentIntentPort,
            DomainInputPortPaymentService domainPaymentServicePort,
            DomainOutputPortPaymentRepository paymentRepository,
            ApplicationKafkaPublisherOutputPort kafkaPublisher) {
        this.domainPaymentIntentPort = domainPaymentIntentPort;
        this.domainPaymentServicePort = domainPaymentServicePort;
        this.paymentRepository = paymentRepository;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Override
    @Transactional
    public void processOrderPlaced(ApplicationOrderPlacedDTO dto) {
        try {
            // Convert DTO to domain command
            DomainCreateIntentCommand command = dto.toDomainCommand();
            
            // Create payment intent in domain
            DomainPaymentIntentEntity intent = domainPaymentIntentPort.createIntent(command);
            
            // Log successful processing
            // Note: Intent is created without payment method at this stage
            // Payment method will be selected later by the customer
            logger.info("Successfully processed OrderPlaced event for order: {}", dto.getOrderId());
        } catch (Exception ex) {
            logger.error("Failed to process OrderPlaced event", ex);
            throw new ApplicationPaymentProcessingException(
                "Failed to process OrderPlaced event",
                ex.getMessage(),
                true // This is retryable
            );
        }
    }

    @Override
    @Transactional
    public void processWebhookUpdate(ApplicationWebhookProcessedDTO dto) {
        logger.info("Processing webhook update for payment: {} with status: {}", dto.getPaymentId(), dto.getNewStatus());
        try {
            // Convert DTO to domain command
            DomainUpdateStatusCommand command = mapToUpdateCommand(dto);
            
            // Update payment status in domain
            domainPaymentServicePort.handleWebhookStatus(command);
            
            // Publish appropriate Kafka events based on the new status
            publishEventBasedOnStatus(dto);
            
            logger.info("Successfully processed webhook update for payment: {}", dto.getPaymentId());
        } catch (Exception ex) {
            logger.error("Failed to process webhook update for payment: " + dto.getPaymentId(), ex);
            throw new ApplicationPaymentProcessingException(
                "Failed to process webhook update",
                ex.getMessage(),
                true // This is retryable
            );
        }
    }
    
    /**
     * Maps DTO to domain update command - Fixed return type
     */
    private DomainUpdateStatusCommand mapToUpdateCommand(ApplicationWebhookProcessedDTO dto) {
        return dto.toDomainCommand();
    }
    
    /**
     * Publishes appropriate Kafka events based on payment status
     */
    private void publishEventBasedOnStatus(ApplicationWebhookProcessedDTO dto) {
        String status = dto.getNewStatus();
        logger.info("Publishing event for payment {} with status: {}", dto.getPaymentId(), status);
        
        if ("SUCCEEDED".equals(status)) {
            // Create and publish payment succeeded event
            DomainPaymentSucceededEvent domainEvent = createPaymentSucceededEvent(dto);
            SharedPaymentSucceededEvent sharedEvent = SharedPaymentSucceededEvent.fromDomainEvent(domainEvent);
            logger.info("Publishing payment succeeded event for payment ID: {}", dto.getPaymentId());
            kafkaPublisher.publishPaymentSucceeded(sharedEvent);
        } else if ("FAILED".equals(status)) {
            // Create and publish payment failed event
            DomainPaymentFailedEvent domainEvent = createPaymentFailedEvent(dto);
            SharedPaymentFailedEvent sharedEvent = SharedPaymentFailedEvent.fromDomainEvent(domainEvent);
            logger.info("Publishing payment failed event for payment ID: {}", dto.getPaymentId());
            kafkaPublisher.publishPaymentFailed(sharedEvent);
        }
    }
    
    /**
     * Creates a domain payment succeeded event from webhook data
     * Fixed to fetch actual payment data instead of using null values
     */
    private DomainPaymentSucceededEvent createPaymentSucceededEvent(ApplicationWebhookProcessedDTO dto) {
        // Fetch the payment entity to get complete data
        DomainPaymentEntity payment = paymentRepository.findPaymentById(new DomainPaymentIdValue(dto.getPaymentId()));
        if (payment == null) {
            logger.error("Payment not found for ID: {}", dto.getPaymentId());
            throw new ApplicationPaymentNotFoundException(dto.getPaymentId());
        }
        
        // Fetch payment intent to get orderId
        DomainPaymentIntentEntity intent = paymentRepository.findPaymentIntentById(payment.getPaymentIntentId());
        
        return new DomainPaymentSucceededEvent(
            payment.getId(),
            payment.getPaymentIntentId(),
            intent != null ? intent.getOrderId() : null,
            payment.getAmount(),
            dto.getTimestamp()
        );
    }
    
    /**
     * Creates a domain payment failed event from webhook data
     * Fixed to fetch actual payment data instead of using null values
     */
    private DomainPaymentFailedEvent createPaymentFailedEvent(ApplicationWebhookProcessedDTO dto) {
        // Fetch the payment entity to get complete data
        DomainPaymentEntity payment = paymentRepository.findPaymentById(new DomainPaymentIdValue(dto.getPaymentId()));
        if (payment == null) {
            logger.error("Payment not found for ID: {}", dto.getPaymentId());
            throw new ApplicationPaymentNotFoundException(dto.getPaymentId());
        }
        
        // Fetch payment intent to get orderId
        DomainPaymentIntentEntity intent = paymentRepository.findPaymentIntentById(payment.getPaymentIntentId());
        
        // Extract failure reason from processor response or use default
        String failureReason = "Payment failed via webhook";
        if (dto.getProcessorResponse() != null && dto.getProcessorResponse().containsKey("failure_reason")) {
            failureReason = dto.getProcessorResponse().get("failure_reason").toString();
        }
        
        return new DomainPaymentFailedEvent(
            payment.getId(),
            payment.getPaymentIntentId(),
            intent != null ? intent.getOrderId() : null,
            failureReason,
            dto.getTimestamp()
        );
    }
}