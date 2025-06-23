package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implements both application and domain event publisher ports using Spring's ApplicationEventPublisher.
 * Handles internal event distribution within the application context.
 */
@Component
public class InfrastructureSpringEventPublisher implements ApplicationEventPublisherOutputPort, DomainOutputPortEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureSpringEventPublisher.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public InfrastructureSpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishPaymentIntentCreated(UUID intentId) {
        log.debug("Publishing PaymentIntentCreated event for intent: {}", intentId);
        PaymentIntentCreatedEvent event = new PaymentIntentCreatedEvent(this, intentId);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishPaymentProcessingStarted(UUID intentId) {
        log.debug("Publishing PaymentProcessingStarted event for intent: {}", intentId);
        PaymentProcessingStartedEvent event = new PaymentProcessingStartedEvent(this, intentId);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishThreeDSecureRequired(UUID intentId, String challengeUrl) {
        log.debug("Publishing ThreeDSecureRequired event for intent: {} with challenge URL {}", intentId, challengeUrl);
        ThreeDSecureRequiredEvent event = new ThreeDSecureRequiredEvent(this, intentId, challengeUrl);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(Object event) {
        log.debug("Publishing generic event: {}", event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAsync(Object event) {
        log.debug("Publishing async event: {}", event.getClass().getSimpleName());
        CompletableFuture.runAsync(() -> applicationEventPublisher.publishEvent(event));
    }

    @Override
    public void publishToTopic(Object event, String topic) {
        log.debug("Publishing event to topic {}: {}", topic, event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // Internal event classes for Spring application events
    public static class PaymentIntentCreatedEvent extends org.springframework.context.ApplicationEvent {
        private final UUID intentId;
        
        public PaymentIntentCreatedEvent(Object source, UUID intentId) {
            super(source);
            this.intentId = intentId;
        }
        
        public UUID getIntentId() {
            return intentId;
        }
    }
    
    public static class PaymentProcessingStartedEvent extends org.springframework.context.ApplicationEvent {
        private final UUID intentId;
        
        public PaymentProcessingStartedEvent(Object source, UUID intentId) {
            super(source);
            this.intentId = intentId;
        }
        
        public UUID getIntentId() {
            return intentId;
        }
    }
    
    public static class ThreeDSecureRequiredEvent extends org.springframework.context.ApplicationEvent {
        private final UUID intentId;
        private final String challengeUrl;
        
        public ThreeDSecureRequiredEvent(Object source, UUID intentId, String challengeUrl) {
            super(source);
            this.intentId = intentId;
            this.challengeUrl = challengeUrl;
        }
        
        public UUID getIntentId() {
            return intentId;
        }
        
        public String getChallengeUrl() {
            return challengeUrl;
        }
    }
}
