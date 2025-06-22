package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.value_objects.DomainWebhookCommand;
import ai.shreds.domain.value_objects.DomainValueWebhookData;
import ai.shreds.domain.ports.DomainOutputPortEventPublisher;
import ai.shreds.domain.ports.DomainOutputPortWebhookRepository;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service orchestrating the full lifecycle of webhook processing.
 */
public class DomainServiceWebhookProcessingService {
    private final DomainServiceSignatureVerificationService signatureVerificationService;
    private final DomainServiceIdempotencyService idempotencyService;
    private final DomainServiceWebhookParserService webhookParserService;
    private final DomainOutputPortWebhookRepository webhookRepository;
    private final DomainServiceCorrelationService correlationService;
    private final DomainOutputPortEventPublisher eventPublisher;

    public DomainServiceWebhookProcessingService(
            DomainServiceSignatureVerificationService signatureVerificationService,
            DomainServiceIdempotencyService idempotencyService,
            DomainServiceWebhookParserService webhookParserService,
            DomainOutputPortWebhookRepository webhookRepository,
            DomainServiceCorrelationService correlationService,
            DomainOutputPortEventPublisher eventPublisher) {
        this.signatureVerificationService = signatureVerificationService;
        this.idempotencyService = idempotencyService;
        this.webhookParserService = webhookParserService;
        this.webhookRepository = webhookRepository;
        this.correlationService = correlationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Processes an incoming webhook command end-to-end: signature verification, idempotency, persistence,
     * parsing, correlation, and event publishing.
     *
     * @param command the incoming webhook data
     * @return the persisted webhook entity
     */
    public DomainEntityPaymentWebhook processWebhook(DomainWebhookCommand command) {
        // Verify signature
        signatureVerificationService.verifyWebhookSignature(command);

        // Check for duplicate
        idempotencyService.checkAndMarkProcessed(command.getExternalEventId(), command.getProcessorType());

        // Persist raw webhook
        var webhook = new DomainEntityPaymentWebhook(
            UUID.randomUUID(),
            command.getProcessorType(),
            command.getExternalEventId(),
            command.getEventType(),
            command.getRawPayload(),
            command.getSignature(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        webhook.markAsVerified();
        DomainEntityPaymentWebhook saved = webhookRepository.save(webhook);

        // Parse and correlate
        DomainValueWebhookData data = webhookParserService.parseWebhookPayload(
            command.getRawPayload(), command.getProcessorType());
        correlationService.correlateWebhookToPayment(saved, data);

        return saved;
    }
}
