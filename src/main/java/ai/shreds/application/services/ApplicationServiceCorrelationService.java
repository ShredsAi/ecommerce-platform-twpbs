package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationWebhookStatusDTO;
import ai.shreds.application.ports.ApplicationInputPortCorrelateWebhooks;
import ai.shreds.application.ports.ApplicationInputPortQueryWebhookStatus;
import ai.shreds.application.exceptions.ApplicationExceptionWebhookNotFoundException;
import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.entities.DomainEntityPaymentStatusUpdate;
import ai.shreds.domain.entities.DomainEntityPayment;
import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import ai.shreds.domain.ports.DomainOutputPortWebhookRepository;
import ai.shreds.domain.ports.DomainOutputPortPaymentQuery;
import ai.shreds.domain.ports.DomainOutputPortStatusUpdateRepository;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortCorrelationService;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import ai.shreds.shared.enums.SharedEnumCorrelationStatus;
import ai.shreds.shared.enums.SharedEnumPaymentEventType;
import ai.shreds.shared.enums.SharedEnumPaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceCorrelationService implements ApplicationInputPortCorrelateWebhooks, ApplicationInputPortQueryWebhookStatus {

    private final DomainOutputPortWebhookRepository webhookRepositoryPort;
    private final DomainOutputPortPaymentQuery paymentQueryPort;
    private final DomainOutputPortStatusUpdateRepository statusUpdatePort;
    private final DomainOutputPortEventPublisher eventPublisherPort;
    private final DomainOutputPortCorrelationService correlationPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void correlatePendingWebhooks() {
        log.info("Starting correlation of pending webhooks");
        
        try {
            // Get all unprocessed status updates
            List<DomainEntityPaymentStatusUpdate> statusUpdates = statusUpdatePort.findUnprocessedUpdates();
            log.info("Found {} unprocessed status updates", statusUpdates.size());
            
            for (DomainEntityPaymentStatusUpdate statusUpdate : statusUpdates) {
                try {
                    processStatusUpdate(statusUpdate);
                } catch (Exception e) {
                    log.error("Error processing status update ID: {}", statusUpdate.getId(), e);
                }
            }
            
            // Also attempt to correlate any webhooks that are still pending
            List<DomainEntityPaymentWebhook> pendingWebhooks = webhookRepositoryPort.findPendingWebhooks();
            log.info("Found {} pending webhooks for correlation retry", pendingWebhooks.size());
            
            for (DomainEntityPaymentWebhook webhook : pendingWebhooks) {
                try {
                    retryWebhookCorrelation(webhook);
                } catch (Exception e) {
                    log.error("Error retrying correlation for webhook ID: {}", webhook.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in correlatePendingWebhooks", e);
            throw new RuntimeException("Failed to correlate pending webhooks", e);
        }
        
        log.info("Completed correlation of pending webhooks");
    }

    @Override
    @Transactional
    public void reconcileUnmatchedWebhooks() {
        log.info("Starting reconciliation of unmatched webhooks");
        
        try {
            List<DomainEntityPaymentWebhook> pendingWebhooks = webhookRepositoryPort.findPendingWebhooks();
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // Consider webhooks older than 24 hours as problematic
            int oldPendingCount = 0;
            
            for (DomainEntityPaymentWebhook webhook : pendingWebhooks) {
                if (webhook.getReceivedAt().isBefore(cutoffTime)) {
                    oldPendingCount++;
                    log.warn("Long-pending webhook detected: ID={}, externalEventId={}, receivedAt={}", 
                            webhook.getId(), webhook.getExternalEventId(), webhook.getReceivedAt());
                    
                    // Could implement alerting mechanism here
                    // For now, we'll just mark very old webhooks as ignored
                    if (webhook.getReceivedAt().isBefore(LocalDateTime.now().minusDays(7))) {
                        webhook.markAsFailed();
                        webhookRepositoryPort.save(webhook);
                        log.info("Marked week-old webhook as failed: ID={}", webhook.getId());
                    }
                }
            }
            
            log.info("Reconciliation completed. Found {} old pending webhooks", oldPendingCount);
            
        } catch (Exception e) {
            log.error("Error in reconcileUnmatchedWebhooks", e);
            throw new RuntimeException("Failed to reconcile unmatched webhooks", e);
        }
    }

    @Override
    public ApplicationWebhookStatusDTO getWebhookStatus(UUID webhookId) {
        log.info("Retrieving webhook status for ID: {}", webhookId);
        
        DomainEntityPaymentWebhook webhook = webhookRepositoryPort.findById(webhookId);
        if (webhook == null) {
            log.warn("Webhook not found: {}", webhookId);
            throw new ApplicationExceptionWebhookNotFoundException(webhookId);
        }
        
        return ApplicationWebhookStatusDTO.builder()
                .webhookId(webhook.getId())
                .processingStatus(webhook.getProcessingStatus())
                .paymentId(webhook.getPaymentId())
                .receivedAt(webhook.getReceivedAt())
                .processedAt(webhook.getProcessedAt())
                .eventType(webhook.getEventType())
                .processorType(webhook.getProcessorType())
                .build();
    }

    private void processStatusUpdate(DomainEntityPaymentStatusUpdate statusUpdate) {
        log.debug("Processing status update: ID={}, paymentId={}", statusUpdate.getId(), statusUpdate.getPaymentId());
        
        try {
            // Find any pending webhooks that might correlate to this payment
            List<DomainEntityPaymentWebhook> pendingWebhooks = webhookRepositoryPort.findPendingWebhooks();
            
            for (DomainEntityPaymentWebhook webhook : pendingWebhooks) {
                if (attemptCorrelationWithPayment(webhook, statusUpdate.getPaymentId())) {
                    log.info("Successfully correlated webhook {} with payment {} via status update", 
                            webhook.getId(), statusUpdate.getPaymentId());
                    break; // Webhook correlated, move to next status update
                }
            }
            
            // Mark status update as processed
            statusUpdate.markAsProcessed();
            statusUpdatePort.markAsProcessed(statusUpdate.getId());
            
        } catch (Exception e) {
            log.error("Error processing status update ID: {}", statusUpdate.getId(), e);
            throw e;
        }
    }

    private void retryWebhookCorrelation(DomainEntityPaymentWebhook webhook) {
        log.debug("Retrying correlation for webhook: ID={}", webhook.getId());
        
        try {
            // Extract processor transaction ID from webhook payload
            String processorTransactionId = extractProcessorTransactionId(webhook.getRawPayload(), webhook.getProcessorType());
            
            if (processorTransactionId != null) {
                DomainEntityPayment payment = paymentQueryPort.findPaymentByProcessorTransactionId(processorTransactionId);
                
                if (payment != null) {
                    correlateWebhookWithPayment(webhook, payment);
                    log.info("Successfully correlated webhook {} with payment {} on retry", webhook.getId(), payment.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error retrying correlation for webhook ID: {}", webhook.getId(), e);
        }
    }

    private boolean attemptCorrelationWithPayment(DomainEntityPaymentWebhook webhook, UUID paymentId) {
        try {
            DomainEntityPayment payment = paymentQueryPort.findPaymentById(paymentId);
            if (payment == null) {
                return false;
            }
            
            // Check if webhook data matches payment data
            String processorTransactionId = extractProcessorTransactionId(webhook.getRawPayload(), webhook.getProcessorType());
            
            if (processorTransactionId != null && payment.getPaymentIntentId().toString().equals(processorTransactionId)) {
                correlateWebhookWithPayment(webhook, payment);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error attempting correlation", e);
            return false;
        }
    }

    private void correlateWebhookWithPayment(DomainEntityPaymentWebhook webhook, DomainEntityPayment payment) {
        // Create correlation record
        DomainEntityPaymentWebhookCorrelation correlation = new DomainEntityPaymentWebhookCorrelation(
                webhook.getId(),
                payment.getId(),
                SharedEnumCorrelationStatus.CORRELATED,
                LocalDateTime.now());
        
        correlationPort.saveCorrelation(correlation);
        
        // Update webhook
        webhook.setPaymentId(payment.getId());
        webhook.markAsProcessed();
        webhookRepositoryPort.save(webhook);
        
        // Publish payment event
        publishPaymentEvent(webhook, payment);
    }

    private void publishPaymentEvent(DomainEntityPaymentWebhook webhook, DomainEntityPayment payment) {
        try {
            SharedEnumPaymentEventType eventType = determineEventType(webhook.getEventType(), payment.getStatus());
            if (eventType == null) {
                log.warn("Could not determine event type for webhook: {}", webhook.getEventType());
                return;
            }

            DomainEntityPaymentEvent paymentEvent = new DomainEntityPaymentEvent(
                    UUID.randomUUID(),
                    eventType,
                    payment.getId(),
                    payment.getPaymentIntentId().toString(),
                    UUID.randomUUID(), // This should come from payment context
                    UUID.randomUUID(), // This should come from payment context
                    payment.getAmount().getAmount(),
                    payment.getAmount().getCurrency(),
                    webhook.getRawPayload(),
                    UUID.randomUUID().toString(), // Correlation ID
                    LocalDateTime.now(),
                    webhook.getId());

            eventPublisherPort.publishPaymentEvent(paymentEvent);
            log.info("Payment event published: eventType={}, paymentId={}", eventType, payment.getId());
            
        } catch (Exception e) {
            log.error("Error publishing payment event for webhook {}", webhook.getId(), e);
        }
    }

    private SharedEnumPaymentEventType determineEventType(String webhookEventType, SharedEnumPaymentStatus paymentStatus) {
        String eventType = webhookEventType.toLowerCase();
        
        // First try to determine from webhook event type
        if (eventType.contains("succeeded") || eventType.contains("completed")) {
            return SharedEnumPaymentEventType.PAYMENT_SUCCEEDED;
        } else if (eventType.contains("failed") || eventType.contains("error")) {
            return SharedEnumPaymentEventType.PAYMENT_FAILED;
        } else if (eventType.contains("refund")) {
            return SharedEnumPaymentEventType.PAYMENT_REFUNDED;
        }
        
        // Fallback to payment status
        switch (paymentStatus) {
            case SUCCEEDED:
                return SharedEnumPaymentEventType.PAYMENT_SUCCEEDED;
            case FAILED:
                return SharedEnumPaymentEventType.PAYMENT_FAILED;
            case REFUNDED:
                return SharedEnumPaymentEventType.PAYMENT_REFUNDED;
            default:
                return null;
        }
    }

    private String extractProcessorTransactionId(String rawPayload, SharedEnumPaymentProcessorType processorType) {
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
}