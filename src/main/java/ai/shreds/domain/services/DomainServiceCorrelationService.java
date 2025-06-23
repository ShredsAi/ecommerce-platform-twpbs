package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityPayment;
import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import ai.shreds.domain.entities.DomainEntityPaymentStatusUpdate;
import ai.shreds.domain.value_objects.DomainValueWebhookData;
import ai.shreds.domain.ports.DomainOutputPortPaymentQuery;
import ai.shreds.domain.ports.DomainOutputPortCorrelationService;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortWebhookRepository;
import ai.shreds.domain.ports.DomainOutputPortStatusUpdateRepository;
import ai.shreds.shared.enums.SharedEnumCorrelationStatus;
import ai.shreds.shared.enums.SharedEnumPaymentEventType;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for correlating webhooks with payments and managing the correlation lifecycle.
 */
public class DomainServiceCorrelationService {
    private final DomainOutputPortPaymentQuery paymentQuery;
    private final DomainOutputPortCorrelationService correlationRepository;
    private final DomainOutputPortEventPublisher eventPublisher;
    private final DomainOutputPortWebhookRepository webhookRepository;
    private final DomainOutputPortStatusUpdateRepository statusUpdateRepository;

    public DomainServiceCorrelationService(
            DomainOutputPortPaymentQuery paymentQuery,
            DomainOutputPortCorrelationService correlationRepository,
            DomainOutputPortEventPublisher eventPublisher,
            DomainOutputPortWebhookRepository webhookRepository,
            DomainOutputPortStatusUpdateRepository statusUpdateRepository) {
        this.paymentQuery = paymentQuery;
        this.correlationRepository = correlationRepository;
        this.eventPublisher = eventPublisher;
        this.webhookRepository = webhookRepository;
        this.statusUpdateRepository = statusUpdateRepository;
    }

    /**
     * Correlates a webhook with its corresponding payment and publishes appropriate events.
     *
     * @param webhook The webhook entity to correlate
     * @param webhookData The parsed webhook data
     * @return The correlation record
     */
    public DomainEntityPaymentWebhookCorrelation correlateWebhookToPayment(
            DomainEntityPaymentWebhook webhook,
            DomainValueWebhookData webhookData) {
        
        // Try to find payment by processor transaction ID
        DomainEntityPayment payment = paymentQuery.findPaymentByProcessorTransactionId(
            webhookData.getProcessorTransactionId());

        if (payment == null) {
            // Create unresolved correlation
            var correlation = new DomainEntityPaymentWebhookCorrelation(
                webhook.getId(),
                null,
                SharedEnumCorrelationStatus.UNRESOLVED,
                LocalDateTime.now()
            );
            correlationRepository.saveCorrelation(correlation);
            return correlation;
        }

        // Create successful correlation
        var correlation = new DomainEntityPaymentWebhookCorrelation(
            webhook.getId(),
            payment.getId(),
            SharedEnumCorrelationStatus.CORRELATED,
            LocalDateTime.now()
        );
        correlationRepository.saveCorrelation(correlation);

        // Update webhook with payment ID
        webhook.setPaymentId(payment.getId());
        webhook.markAsProcessed();
        webhookRepository.save(webhook);

        // Publish appropriate payment event
        publishPaymentEvent(payment, webhook, webhookData);

        return correlation;
    }

    /**
     * Publishes appropriate payment event based on payment status.
     */
    private void publishPaymentEvent(DomainEntityPayment payment, DomainEntityPaymentWebhook webhook, DomainValueWebhookData webhookData) {
        SharedEnumPaymentEventType eventType;
        
        // Determine event type based on webhook data status or payment status
        String status = webhookData.getStatus().toLowerCase();
        if (status.contains("succeeded") || status.contains("completed") || status.contains("captured")) {
            eventType = SharedEnumPaymentEventType.PAYMENT_SUCCEEDED;
        } else if (status.contains("failed") || status.contains("declined") || status.contains("canceled")) {
            eventType = SharedEnumPaymentEventType.PAYMENT_FAILED;
        } else if (status.contains("refund") || status.contains("reversed")) {
            eventType = SharedEnumPaymentEventType.PAYMENT_REFUNDED;
        } else {
            return; // No event for other statuses
        }

        // Extract customer and order IDs from webhook data
        UUID customerId = null;
        UUID orderId = null;
        
        if (webhookData.getCustomerId() != null && !webhookData.getCustomerId().isEmpty()) {
            try {
                customerId = UUID.fromString(webhookData.getCustomerId());
            } catch (IllegalArgumentException e) {
                // Customer ID is not a valid UUID, keep as null
            }
        }
        
        if (webhookData.getOrderId() != null && !webhookData.getOrderId().isEmpty()) {
            try {
                orderId = UUID.fromString(webhookData.getOrderId());
            } catch (IllegalArgumentException e) {
                // Order ID is not a valid UUID, keep as null
            }
        }

        var event = new DomainEntityPaymentEvent(
            UUID.randomUUID(),
            eventType,
            payment.getId(),
            payment.getPaymentIntentId().toString(),
            customerId,
            orderId,
            webhookData.getAmount().getAmount(),
            webhookData.getAmount().getCurrency(),
            webhook.getRawPayload(), // Use raw payload as event data
            UUID.randomUUID().toString(), // Generate new correlation ID
            LocalDateTime.now(),
            webhook.getId()
        );

        eventPublisher.publishPaymentEvent(event);
    }

    /**
     * Processes pending correlations by checking for newly available payment data.
     * This method is called by scheduled jobs to resolve previously unmatched webhooks.
     */
    public void processPendingCorrelations() {
        // Get all unprocessed status updates
        List<DomainEntityPaymentStatusUpdate> unprocessedUpdates = statusUpdateRepository.findUnprocessedUpdates();
        
        for (DomainEntityPaymentStatusUpdate update : unprocessedUpdates) {
            try {
                // Find pending webhooks that might correlate with this payment
                List<DomainEntityPaymentWebhook> pendingWebhooks = webhookRepository.findPendingWebhooks();
                
                for (DomainEntityPaymentWebhook webhook : pendingWebhooks) {
                    // Check if this webhook has an unresolved correlation
                    DomainEntityPaymentWebhookCorrelation existingCorrelation = 
                        correlationRepository.findCorrelationByWebhookId(webhook.getId());
                    
                    if (existingCorrelation != null && 
                        existingCorrelation.getCorrelationStatus() == SharedEnumCorrelationStatus.UNRESOLVED) {
                        
                        // Try to find the payment for this webhook
                        DomainEntityPayment payment = paymentQuery.findPaymentById(update.getPaymentId());
                        
                        if (payment != null) {
                            // Re-parse webhook data to check if it matches this payment
                            DomainServiceWebhookParserService parserService = new DomainServiceWebhookParserService();
                            DomainValueWebhookData webhookData = parserService.parseWebhookPayload(
                                webhook.getRawPayload(), webhook.getProcessorType());
                            
                            // Check if the webhook data matches this payment
                            if (doesWebhookMatchPayment(webhookData, payment)) {
                                // Update correlation to CORRELATED
                                var newCorrelation = new DomainEntityPaymentWebhookCorrelation(
                                    webhook.getId(),
                                    payment.getId(),
                                    SharedEnumCorrelationStatus.CORRELATED,
                                    LocalDateTime.now()
                                );
                                correlationRepository.saveCorrelation(newCorrelation);
                                
                                // Update webhook
                                webhook.setPaymentId(payment.getId());
                                webhook.markAsProcessed();
                                webhookRepository.save(webhook);
                                
                                // Publish payment event
                                publishPaymentEvent(payment, webhook, webhookData);
                                
                                break; // Found correlation for this webhook
                            }
                        }
                    }
                }
                
                // Mark status update as processed
                update.markAsProcessed();
                statusUpdateRepository.markAsProcessed(update.getId());
                
            } catch (Exception e) {
                // Log error and continue processing other updates
                // In a real implementation, we would use proper logging
                System.err.println("Error processing status update " + update.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks if webhook data matches a payment entity.
     */
    private boolean doesWebhookMatchPayment(DomainValueWebhookData webhookData, DomainEntityPayment payment) {
        // Match by payment intent ID
        if (webhookData.getPaymentIntentId() != null && 
            webhookData.getPaymentIntentId().equals(payment.getPaymentIntentId().toString())) {
            return true;
        }
        
        // Match by processor transaction ID if available
        if (webhookData.getProcessorTransactionId() != null && 
            payment.getProcessorResponse() != null &&
            webhookData.getProcessorTransactionId().equals(payment.getProcessorResponse().getProcessorId())) {
            return true;
        }
        
        // Match by amount and approximate timestamp (within reasonable window)
        if (webhookData.getAmount().equals(payment.getAmount()) &&
            isTimestampWithinWindow(webhookData.getTimestamp(), payment.getProcessedAt())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if two timestamps are within a reasonable correlation window (e.g., 1 hour).
     */
    private boolean isTimestampWithinWindow(LocalDateTime timestamp1, LocalDateTime timestamp2) {
        if (timestamp1 == null || timestamp2 == null) {
            return false;
        }
        
        long minutesDiff = Math.abs(java.time.Duration.between(timestamp1, timestamp2).toMinutes());
        return minutesDiff <= 60; // Within 1 hour
    }
}
