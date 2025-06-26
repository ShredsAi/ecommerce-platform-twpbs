package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing payment details for an order.
 */
public class DomainPaymentDetailsEntity {
    private final DomainOrderIdValue orderId;
    private SharedPaymentStatusEnum paymentStatus;
    private final String transactionId;
    private final String authorizationCode;
    private final SharedMoneyValue amount;
    private final String currency;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * All-args constructor made public for MapStruct instantiation.
     */
    public DomainPaymentDetailsEntity(DomainOrderIdValue orderId,
                                      SharedPaymentStatusEnum paymentStatus,
                                      String transactionId,
                                      String authorizationCode,
                                      SharedMoneyValue amount,
                                      String currency,
                                      Instant createdAt,
                                      Instant updatedAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (paymentStatus == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.authorizationCode = authorizationCode;
        this.amount = amount;
        this.currency = currency != null ? currency : amount.getCurrency();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }
    
    /**
     * Factory method to create payment details for a new transaction.
     */
    public static DomainPaymentDetailsEntity create(DomainOrderIdValue orderId,
                                                  String transactionId,
                                                  String authorizationCode,
                                                  SharedMoneyValue amount) {
        return new DomainPaymentDetailsEntity(
            orderId,
            SharedPaymentStatusEnum.INITIATED,
            transactionId,
            authorizationCode,
            amount,
            amount.getCurrency(),
            Instant.now(),
            Instant.now()
        );
    }
    
    /**
     * Factory method to create payment details from shared data.
     */
    public static DomainPaymentDetailsEntity fromSharedData(UUID orderId,
                                                          String status,
                                                          String transactionId,
                                                          String authorizationCode,
                                                          SharedMoneyValue amount) {
        DomainOrderIdValue orderIdValue = new DomainOrderIdValue(orderId);
        SharedPaymentStatusEnum paymentStatus = SharedPaymentStatusEnum.valueOf(status);
        
        return new DomainPaymentDetailsEntity(
            orderIdValue,
            paymentStatus,
            transactionId,
            authorizationCode,
            amount,
            amount.getCurrency(),
            Instant.now(),
            Instant.now()
        );
    }
    
    /**
     * Factory method to create authorized payment details.
     */
    public static DomainPaymentDetailsEntity createAuthorized(DomainOrderIdValue orderId,
                                                            String transactionId,
                                                            String authorizationCode,
                                                            SharedMoneyValue amount) {
        return new DomainPaymentDetailsEntity(
            orderId,
            SharedPaymentStatusEnum.AUTHORIZED,
            transactionId,
            authorizationCode,
            amount,
            amount.getCurrency(),
            Instant.now(),
            Instant.now()
        );
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public SharedPaymentStatusEnum getPaymentStatus() {
        return paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public SharedMoneyValue getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Update the payment status and timestamp.
     * @param newStatus The new payment status
     */
    public void updateStatus(SharedPaymentStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        this.paymentStatus = newStatus;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if the payment is in a final state (completed or failed).
     * @return true if payment is in final state
     */
    public boolean isInFinalState() {
        return paymentStatus == SharedPaymentStatusEnum.CAPTURED ||
               paymentStatus == SharedPaymentStatusEnum.REFUNDED ||
               paymentStatus == SharedPaymentStatusEnum.FAILED;
    }
    
    /**
     * Check if the payment can be captured.
     * @return true if payment can be captured
     */
    public boolean canBeCaptured() {
        return paymentStatus == SharedPaymentStatusEnum.AUTHORIZED;
    }
    
    /**
     * Check if the payment can be refunded.
     * @return true if payment can be refunded
     */
    public boolean canBeRefunded() {
        return paymentStatus == SharedPaymentStatusEnum.CAPTURED;
    }
}