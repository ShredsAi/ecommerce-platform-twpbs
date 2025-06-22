package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainValueProcessorResponse;
import ai.shreds.shared.enums.SharedEnumPaymentStatus;
import ai.shreds.shared.value_objects.SharedValueMoney;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity representing the result of a processed payment intent
 */
@Entity
@Table(name = "payments")
public class DomainEntityPayment {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "payment_intent_id", nullable = false)
    private UUID paymentIntentId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private SharedValueMoney amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SharedEnumPaymentStatus status;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "processorId", column = @Column(name = "processor_id")),
        @AttributeOverride(name = "responseCode", column = @Column(name = "response_code")),
        @AttributeOverride(name = "responseMessage", column = @Column(name = "response_message")),
        @AttributeOverride(name = "rawResponse", column = @Column(name = "raw_response", columnDefinition = "TEXT"))
    })
    private DomainValueProcessorResponse processorResponse;
    
    @Column(name = "processor_transaction_id")
    private String processorTransactionId;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Default constructor for JPA
    protected DomainEntityPayment() {}
    
    /**
     * Creates a new payment entity
     */
    public DomainEntityPayment(
            UUID id,
            UUID paymentIntentId,
            SharedValueMoney amount,
            SharedEnumPaymentStatus status,
            DomainValueProcessorResponse processorResponse,
            String processorTransactionId,
            LocalDateTime processedAt) {
        this.id = id;
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
        this.status = status;
        this.processorResponse = processorResponse;
        this.processorTransactionId = processorTransactionId;
        this.processedAt = processedAt;
    }
    
    /**
     * Checks if the payment was successful
     * @return true if payment status is SUCCEEDED
     */
    public boolean isSuccessful() {
        return status == SharedEnumPaymentStatus.SUCCEEDED;
    }
    
    /**
     * Checks if the payment failed
     * @return true if payment status is FAILED
     */
    public boolean isFailed() {
        return status == SharedEnumPaymentStatus.FAILED;
    }
    
    /**
     * Gets the reason for payment failure
     * @return the failure reason message if available, or empty string
     */
    public String getFailureReason() {
        if (!isFailed()) {
            return "";
        }
        return processorResponse.getResponseMessage();
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }

    public UUID getPaymentIntentId() {
        return paymentIntentId;
    }

    public SharedValueMoney getAmount() {
        return amount;
    }

    public SharedEnumPaymentStatus getStatus() {
        return status;
    }

    public DomainValueProcessorResponse getProcessorResponse() {
        return processorResponse;
    }
    
    public String getProcessorTransactionId() {
        return processorTransactionId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}