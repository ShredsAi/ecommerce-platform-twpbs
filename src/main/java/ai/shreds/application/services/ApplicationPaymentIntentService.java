package ai.shreds.application.services;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.application.dtos.ApplicationCreatePaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationPaymentConfirmationDTO;
import ai.shreds.application.dtos.ApplicationPaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationConfirmPaymentIntentDTO;
import ai.shreds.application.exceptions.ApplicationPaymentProcessingException;
import ai.shreds.application.ports.ApplicationCreatePaymentIntentInputPort;
import ai.shreds.application.ports.ApplicationConfirmPaymentIntentInputPort;
import ai.shreds.application.ports.ApplicationKafkaPublisherOutputPort;
import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.commands.DomainConfirmIntentCommand;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;
import ai.shreds.domain.ports.DomainInputPortPaymentIntent;
import ai.shreds.domain.ports.DomainInputPortPaymentService;
import ai.shreds.domain.ports.DomainInputPortThreeDSecure;
import ai.shreds.domain.services.DomainPaymentProcessorRouter;
import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.events.DomainPaymentSucceededEvent;
import ai.shreds.domain.events.DomainPaymentFailedEvent;
import ai.shreds.shared.dtos.SharedPaymentSucceededEvent;
import ai.shreds.shared.dtos.SharedPaymentFailedEvent;

/**
 * Application service handling payment intent creation and confirmation
 */
@Service
public class ApplicationPaymentIntentService implements ApplicationCreatePaymentIntentInputPort, ApplicationConfirmPaymentIntentInputPort {

    private final DomainInputPortPaymentIntent domainPaymentIntentPort;
    private final DomainPaymentProcessorRouter paymentProcessorRouter;
    private final DomainInputPortPaymentService paymentService;
    private final DomainInputPortThreeDSecure threeDSecureService;
    private final ApplicationKafkaPublisherOutputPort kafkaPublisher;
    private final ApplicationEventPublisherOutputPort eventPublisher;

    public ApplicationPaymentIntentService(
            DomainInputPortPaymentIntent domainPaymentIntentPort,
            DomainPaymentProcessorRouter paymentProcessorRouter,
            DomainInputPortPaymentService paymentService,
            DomainInputPortThreeDSecure threeDSecureService,
            ApplicationKafkaPublisherOutputPort kafkaPublisher,
            ApplicationEventPublisherOutputPort eventPublisher) {
        this.domainPaymentIntentPort = domainPaymentIntentPort;
        this.paymentProcessorRouter = paymentProcessorRouter;
        this.paymentService = paymentService;
        this.threeDSecureService = threeDSecureService;
        this.kafkaPublisher = kafkaPublisher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ApplicationPaymentIntentDTO createPaymentIntent(ApplicationCreatePaymentIntentDTO dto) {
        DomainCreateIntentCommand cmd = dto.toDomainCommand();
        DomainPaymentIntentEntity intent = domainPaymentIntentPort.createIntent(cmd);
        
        // Publish domain event for intent created
        eventPublisher.publishPaymentIntentCreated(intent.getId().getValue());
        
        return ApplicationPaymentIntentDTO.fromDomainEntity(intent);
    }

    @Override
    @Transactional
    public ApplicationPaymentConfirmationDTO confirmPaymentIntent(UUID id, ApplicationConfirmPaymentIntentDTO dto) {
        // Map to domain command and confirm intent
        DomainConfirmIntentCommand cmd = dto.toDomainCommand(id);
        DomainPaymentIntentEntity intent = domainPaymentIntentPort.confirmIntent(cmd);
        
        // Publish processing started
        eventPublisher.publishPaymentProcessingStarted(intent.getId().getValue());
        
        // Execute charge via processor router
        DomainProcessorChargeResult result = paymentProcessorRouter.routeAndCharge(intent);
        
        // Handle 3DS required
        if (result.isRequiresAction()) {
            // Extract challenge URL from proper structure
            String challengeUrl = "";
            Map<String, Object> nextAction = result.getNextAction();
            if (nextAction != null && nextAction.containsKey("redirect_to_url")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> redirectInfo = (Map<String, Object>) nextAction.get("redirect_to_url");
                if (redirectInfo != null && redirectInfo.containsKey("url")) {
                    challengeUrl = redirectInfo.get("url").toString();
                }
            }
            
            // Initiate 3DS process in domain
            DomainThreeDSecureEntity threeDSecure = threeDSecureService.initiate3DS(intent);
            
            // Publish event for 3DS requirement
            eventPublisher.publishThreeDSecureRequired(intent.getId().getValue(), challengeUrl);
            
            // Return DTO with 3DS information
            return ApplicationPaymentConfirmationDTO.createForThreeDSecure(intent, threeDSecure);
        }
        
        // Record payment in domain
        DomainPaymentEntity payment;
        try {
            payment = paymentService.recordPayment(intent, result);
        } catch (Exception ex) {
            throw new ApplicationPaymentProcessingException(
                    "Error recording payment",
                    ex.getMessage(),
                    false);
        }
        
        // Create domain events and publish external Kafka events
        if (payment.isSuccessful()) {
            DomainPaymentSucceededEvent domainEvent = new DomainPaymentSucceededEvent(
                payment.getId(),
                payment.getPaymentIntentId(),
                intent.getOrderId(),
                payment.getAmount(),
                payment.getProcessedAt()
            );
            SharedPaymentSucceededEvent sharedEvent = SharedPaymentSucceededEvent.fromDomainEvent(domainEvent);
            kafkaPublisher.publishPaymentSucceeded(sharedEvent);
        } else {
            DomainPaymentFailedEvent domainEvent = new DomainPaymentFailedEvent(
                payment.getId(),
                payment.getPaymentIntentId(), 
                intent.getOrderId(),
                payment.getFailureReason(),
                payment.getProcessedAt()
            );
            SharedPaymentFailedEvent sharedEvent = SharedPaymentFailedEvent.fromDomainEvent(domainEvent);
            kafkaPublisher.publishPaymentFailed(sharedEvent);
        }
        
        // Return application DTO
        return ApplicationPaymentConfirmationDTO.fromDomainEntity(intent, payment);
    }
}