package ai.shreds.application.dtos;

import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedProcessorResponseValue;
import ai.shreds.domain.entities.DomainPaymentEntity;

/**
 * DTO representing payment details in the application layer
 */
public class ApplicationPaymentDetailsDTO {

    private UUID id;
    private String status;
    private SharedMoneyValue amount;
    private UUID paymentIntentId;
    private SharedProcessorResponseValue processorResponse;
    private LocalDateTime processedAt;

    public ApplicationPaymentDetailsDTO() {}

    public ApplicationPaymentDetailsDTO(UUID id, String status, SharedMoneyValue amount, UUID paymentIntentId,
            SharedProcessorResponseValue processorResponse, LocalDateTime processedAt) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.paymentIntentId = paymentIntentId;
        this.processorResponse = processorResponse;
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SharedMoneyValue getAmount() {
        return amount;
    }

    public void setAmount(SharedMoneyValue amount) {
        this.amount = amount;
    }

    public UUID getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(UUID paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public SharedProcessorResponseValue getProcessorResponse() {
        return processorResponse;
    }

    public void setProcessorResponse(SharedProcessorResponseValue processorResponse) {
        this.processorResponse = processorResponse;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    /**
     * Creates an ApplicationPaymentDetailsDTO from a domain entity
     */
    public static ApplicationPaymentDetailsDTO fromDomainEntity(DomainPaymentEntity payment) {
        return new ApplicationPaymentDetailsDTO(
            payment.getId().getValue(),
            payment.getStatus().name(),
            SharedMoneyValue.fromDomainValue(payment.getAmount()),
            payment.getPaymentIntentId().getValue(),
            SharedProcessorResponseValue.fromDomainValue(payment.getProcessorResponse()),
            payment.getProcessedAt()
        );
    }
}