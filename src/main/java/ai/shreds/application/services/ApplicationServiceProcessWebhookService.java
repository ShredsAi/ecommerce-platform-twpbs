package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationWebhookDTO;
import ai.shreds.application.dtos.ApplicationWebhookResultDTO;
import ai.shreds.application.dtos.ApplicationCorrelationResultDTO;
import ai.shreds.application.ports.ApplicationInputPortProcessWebhook;
import ai.shreds.application.exceptions.ApplicationExceptionWebhookProcessingFailedException;
import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import ai.shreds.domain.entities.DomainEntityPayment;
import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import ai.shreds.domain.ports.DomainOutputPortSignatureVerifier;
import ai.shreds.domain.ports.DomainOutputPortIdempotencyService;
import ai.shreds.domain.ports.DomainOutputPortWebhookRepository;
import ai.shreds.domain.ports.DomainOutputPortCorrelationService;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortPaymentQuery;
import ai.shreds.domain.value_objects.DomainWebhookCommand;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import ai.shreds.shared.enums.SharedEnumCorrelationStatus;
import ai.shreds.shared.enums.SharedEnumPaymentEventType;
import ai.shreds.shared.exceptions.SharedExceptionWebhookValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceProcessWebhookService implements ApplicationInputPortProcessWebhook {

    private final DomainOutputPortSignatureVerifier signatureVerifierPort;
    private final DomainOutputPortIdempotencyService idempotencyPort;
    private final DomainOutputPortWebhookRepository webhookRepositoryPort;
    private final DomainOutputPortCorrelationService correlationPort;
    private final DomainOutputPortEventPublisher eventPublisherPort;
    private final DomainOutputPortPaymentQuery paymentQueryPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApplicationWebhookResultDTO processWebhook(ApplicationWebhookDTO webhookDTO) {
        log.info("Processing webhook: externalEventId={}, processorType={}", 
                webhookDTO.getExternalEventId(), webhookDTO.getProcessorType());

        try {
            // Convert DTO to Domain Command
            DomainWebhookCommand command = webhookDTO.toDomainCommand();

            // 1. Check for duplicate webhook (idempotency)
            if (idempotencyPort.isDuplicate(command.getExternalEventId(), command.getProcessorType())) {
                log.info("Duplicate webhook detected: externalEventId={}", command.getExternalEventId());
                DomainEntityPaymentWebhook existingWebhook = webhookRepositoryPort
                        .findByExternalEventIdAndProcessorType(command.getExternalEventId(), command.getProcessorType());
                return mapToResultDTO(existingWebhook, null);
            }

            // 2. Verify webhook signature
            boolean isSignatureValid = signatureVerifierPort.verifySignature(
                    command.getRawPayload(), 
                    command.getSignature(), 
                    command.getProcessorType());
            
            if (!isSignatureValid) {
                log.error("Invalid webhook signature for externalEventId={}", command.getExternalEventId());
                throw new SharedExceptionWebhookValidationException(
                        command.getProcessorType(),
                        "Invalid signature",
                        null);
            }

            // 3. Create and persist webhook entity
            DomainEntityPaymentWebhook webhook = new DomainEntityPaymentWebhook(
                    UUID.randomUUID(),
                    command.getProcessorType(),
                    command.getExternalEventId(),
                    command.getEventType(),
                    command.getRawPayload(),
                    command.getSignature(),
                    LocalDateTime.now(),
                    LocalDateTime.now());
            
            webhook.markAsVerified();
            webhook = webhookRepositoryPort.save(webhook);
            log.info("Webhook persisted with ID: {}", webhook.getId());

            // 4. Parse webhook payload and attempt correlation
            ApplicationCorrelationResultDTO correlationResult = attemptCorrelation(webhook, command);
            
            // 5. Update webhook status based on correlation result
            if (correlationResult.getCorrelationStatus() == SharedEnumCorrelationStatus.CORRELATED) {
                webhook.setPaymentId(correlationResult.getPaymentId());
                webhook.markAsProcessed();
                
                // Publish payment event
                publishPaymentEvent(webhook, correlationResult.getPaymentId());
            } else if (correlationResult.getCorrelationStatus() == SharedEnumCorrelationStatus.ERROR) {
                webhook.markAsFailed();
            }
            // If UNRESOLVED, webhook remains PENDING for later correlation
            
            webhookRepositoryPort.save(webhook);
            
            return mapToResultDTO(webhook, correlationResult);
            
        } catch (Exception e) {
            log.error("Error processing webhook: externalEventId={}", webhookDTO.getExternalEventId(), e);
            throw new ApplicationExceptionWebhookProcessingFailedException(
                    null, "Webhook processing failed", e);
        }
    }

    private ApplicationCorrelationResultDTO attemptCorrelation(DomainEntityPaymentWebhook webhook, DomainWebhookCommand command) {
        try {
            // Parse webhook payload to extract payment information
            String processorTransactionId = extractProcessorTransactionId(command.getRawPayload(), command.getProcessorType());
            
            if (processorTransactionId == null) {
                log.warn("Could not extract processor transaction ID from webhook payload");
                return ApplicationCorrelationResultDTO.builder()
                        .correlationStatus(SharedEnumCorrelationStatus.UNRESOLVED)
                        .correlatedAt(LocalDateTime.now())
                        .failureReason("Could not extract processor transaction ID")
                        .build();
            }

            // Try to find existing payment by processor transaction ID
            DomainEntityPayment payment = paymentQueryPort.findPaymentByProcessorTransactionId(processorTransactionId);
            
            if (payment != null) {
                // Create correlation record
                DomainEntityPaymentWebhookCorrelation correlation = new DomainEntityPaymentWebhookCorrelation(
                        webhook.getId(),
                        payment.getId(),
                        SharedEnumCorrelationStatus.CORRELATED,
                        LocalDateTime.now());
                
                correlationPort.saveCorrelation(correlation);
                log.info("Webhook correlated successfully: webhookId={}, paymentId={}", webhook.getId(), payment.getId());
                
                return ApplicationCorrelationResultDTO.builder()
                        .correlationStatus(SharedEnumCorrelationStatus.CORRELATED)
                        .paymentId(payment.getId())
                        .correlatedAt(LocalDateTime.now())
                        .build();
            } else {
                log.info("Payment not found for processor transaction ID: {}", processorTransactionId);
                return ApplicationCorrelationResultDTO.builder()
                        .correlationStatus(SharedEnumCorrelationStatus.UNRESOLVED)
                        .correlatedAt(LocalDateTime.now())
                        .failureReason("Payment not found")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error during correlation attempt", e);
            return ApplicationCorrelationResultDTO.builder()
                    .correlationStatus(SharedEnumCorrelationStatus.ERROR)
                    .correlatedAt(LocalDateTime.now())
                    .failureReason("Correlation error: " + e.getMessage())
                    .build();
        }
    }

    private String extractProcessorTransactionId(String rawPayload, ai.shreds.shared.enums.SharedEnumPaymentProcessorType processorType) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            
            switch (processorType) {
                case STRIPE:
                    JsonNode stripeData = root.path("data").path("object");
                    return stripeData.path("id").asText(null);
                case PAYPAL:
                    return root.path("resource").path("id").asText(null);
                case SQUARE:
                    return root.path("data").path("id").asText(null);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error extracting processor transaction ID", e);
            return null;
        }
    }

    private void publishPaymentEvent(DomainEntityPaymentWebhook webhook, UUID paymentId) {
        try {
            DomainEntityPayment payment = paymentQueryPort.findPaymentById(paymentId);
            if (payment == null) {
                log.warn("Payment not found for event publishing: {}", paymentId);
                return;
            }

            SharedEnumPaymentEventType eventType = determineEventType(webhook.getEventType());
            if (eventType == null) {
                log.warn("Could not determine event type for: {}", webhook.getEventType());
                return;
            }

            DomainEntityPaymentEvent paymentEvent = new DomainEntityPaymentEvent(
                    UUID.randomUUID(),
                    eventType,
                    paymentId,
                    payment.getPaymentIntentId().toString(),
                    UUID.randomUUID(), // This should come from payment data
                    UUID.randomUUID(), // This should come from payment data  
                    payment.getAmount().getAmount(),
                    payment.getAmount().getCurrency(),
                    webhook.getRawPayload(),
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    webhook.getId());

            eventPublisherPort.publishPaymentEvent(paymentEvent);
            log.info("Payment event published: eventType={}, paymentId={}", eventType, paymentId);
            
        } catch (Exception e) {
            log.error("Error publishing payment event", e);
        }
    }

    private SharedEnumPaymentEventType determineEventType(String webhookEventType) {
        String eventType = webhookEventType.toLowerCase();
        if (eventType.contains("succeeded") || eventType.contains("completed")) {
            return SharedEnumPaymentEventType.PAYMENT_SUCCEEDED;
        } else if (eventType.contains("failed") || eventType.contains("error")) {
            return SharedEnumPaymentEventType.PAYMENT_FAILED;
        } else if (eventType.contains("refund")) {
            return SharedEnumPaymentEventType.PAYMENT_REFUNDED;
        }
        return null;
    }

    private ApplicationWebhookResultDTO mapToResultDTO(DomainEntityPaymentWebhook webhook, ApplicationCorrelationResultDTO correlationResult) {
        return ApplicationWebhookResultDTO.builder()
                .webhookId(webhook.getId())
                .status(webhook.getProcessingStatus())
                .correlationResult(correlationResult)
                .processedAt(webhook.getProcessedAt() != null ? webhook.getProcessedAt() : LocalDateTime.now())
                .build();
    }
}