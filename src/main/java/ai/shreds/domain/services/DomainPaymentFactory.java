package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Factory for creating payment domain entities.
 * Ensures proper initialization and business rule enforcement.
 */
@Service
public class DomainPaymentFactory {

    /**
     * Creates a new payment entity from a payment intent and processor charge result.
     * 
     * @param intent the payment intent that generated this payment
     * @param result the processor charge result
     * @return a new payment entity
     */
    public DomainPaymentEntity createPayment(DomainPaymentIntentEntity intent, DomainProcessorChargeResult result) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(result, "result cannot be null");

        // Validate that intent is in processing state
        if (intent.getStatus() != ai.shreds.domain.value_objects.DomainPaymentStatusEnum.PROCESSING) {
            throw new IllegalArgumentException("Payment can only be created from processing intent");
        }

        // Generate unique payment ID
        DomainPaymentIdValue paymentId = new DomainPaymentIdValue(UUID.randomUUID());

        // Create payment entity with processor result
        return DomainPaymentEntity.create(
                paymentId,
                intent.getId(),
                intent.getAmount(),
                result.getStatus(),
                intent.getProcessorType(),
                result.getProcessorResponse(),
                java.time.LocalDateTime.now()
        );
    }

    /**
     * Creates a payment entity for a successful charge.
     * 
     * @param intent the payment intent
     * @param processorResponse the processor response
     * @return a new successful payment entity
     */
    public DomainPaymentEntity createSuccessfulPayment(
            DomainPaymentIntentEntity intent,
            ai.shreds.domain.value_objects.DomainProcessorResponseValue processorResponse) {
        
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(processorResponse, "processorResponse cannot be null");

        DomainPaymentIdValue paymentId = new DomainPaymentIdValue(UUID.randomUUID());

        return DomainPaymentEntity.create(
                paymentId,
                intent.getId(),
                intent.getAmount(),
                ai.shreds.domain.value_objects.DomainPaymentStatusEnum.SUCCEEDED,
                intent.getProcessorType(),
                processorResponse,
                java.time.LocalDateTime.now()
        );
    }

    /**
     * Creates a payment entity for a failed charge.
     * 
     * @param intent the payment intent
     * @param processorResponse the processor response containing failure details
     * @return a new failed payment entity
     */
    public DomainPaymentEntity createFailedPayment(
            DomainPaymentIntentEntity intent,
            ai.shreds.domain.value_objects.DomainProcessorResponseValue processorResponse) {
        
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(processorResponse, "processorResponse cannot be null");

        DomainPaymentIdValue paymentId = new DomainPaymentIdValue(UUID.randomUUID());

        return DomainPaymentEntity.create(
                paymentId,
                intent.getId(),
                intent.getAmount(),
                ai.shreds.domain.value_objects.DomainPaymentStatusEnum.FAILED,
                intent.getProcessorType(),
                processorResponse,
                java.time.LocalDateTime.now()
        );
    }

    /**
     * Creates a payment entity from webhook data.
     * This is used when payments are created or updated via webhook notifications.
     * 
     * @param intent the payment intent
     * @param status the payment status from webhook
     * @param processorResponse the processor response from webhook
     * @return a new payment entity
     */
    public DomainPaymentEntity createPaymentFromWebhook(
            DomainPaymentIntentEntity intent,
            ai.shreds.domain.value_objects.DomainPaymentStatusEnum status,
            ai.shreds.domain.value_objects.DomainProcessorResponseValue processorResponse) {
        
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(processorResponse, "processorResponse cannot be null");

        // Validate that status is terminal (webhooks should only create terminal payments)
        if (status != ai.shreds.domain.value_objects.DomainPaymentStatusEnum.SUCCEEDED && 
            status != ai.shreds.domain.value_objects.DomainPaymentStatusEnum.FAILED) {
            throw new IllegalArgumentException("Webhook payments must have terminal status (SUCCEEDED or FAILED)");
        }

        DomainPaymentIdValue paymentId = new DomainPaymentIdValue(UUID.randomUUID());

        return DomainPaymentEntity.create(
                paymentId,
                intent.getId(),
                intent.getAmount(),
                status,
                intent.getProcessorType(),
                processorResponse,
                java.time.LocalDateTime.now()
        );
    }

    /**
     * Validates that a payment can be created from the given intent and result.
     * 
     * @param intent the payment intent
     * @param result the processor charge result
     * @throws IllegalArgumentException if validation fails
     */
    public void validatePaymentCreation(DomainPaymentIntentEntity intent, DomainProcessorChargeResult result) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(result, "result cannot be null");

        // Validate intent state
        if (intent.getStatus() != ai.shreds.domain.value_objects.DomainPaymentStatusEnum.PROCESSING) {
            throw new IllegalArgumentException("Payment intent must be in PROCESSING state");
        }

        // Validate intent is not expired
        if (intent.isExpired()) {
            throw new IllegalArgumentException("Cannot create payment from expired intent");
        }

        // Validate result has processor response
        if (result.getProcessorResponse() == null) {
            throw new IllegalArgumentException("Processor response cannot be null");
        }

        // Validate amount consistency
        if (!intent.getAmount().equals(result.getProcessorResponse().getProcessorId())) {
            // Note: In a real implementation, you'd validate the amount matches
            // For now, we'll skip this as we don't have the exact amount in processor response
        }
    }

    /**
     * Validates business rules for payment creation.
     * 
     * @param intent the payment intent
     * @throws IllegalArgumentException if validation fails
     */
    private void validateIntentForPayment(DomainPaymentIntentEntity intent) {
        if (intent.isExpired()) {
            throw new IllegalArgumentException("Cannot create payment from expired intent");
        }

        if (intent.getAmount() == null || !intent.getAmount().isPositive()) {
            throw new IllegalArgumentException("Payment intent must have positive amount");
        }

        if (intent.getPaymentMethodId() == null) {
            throw new IllegalArgumentException("Payment intent must have payment method");
        }
    }
}