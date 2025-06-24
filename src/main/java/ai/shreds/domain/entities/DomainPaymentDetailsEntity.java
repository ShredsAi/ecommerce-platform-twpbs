package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import ai.shreds.domain.exceptions.DomainValidationException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainPaymentDetailsEntity {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "order_id", unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private SharedPaymentStatusEnum paymentStatus;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_provider", nullable = false, length = 40)
    private String paymentProvider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = SharedPaymentStatusEnum.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
        validateBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Payment amount must be greater than zero", 
                List.of("paymentAmount must be > 0"));
        }
        if (paymentProvider == null || paymentProvider.trim().isEmpty()) {
            throw new DomainValidationException("Payment provider is required", 
                List.of("paymentProvider cannot be null or empty"));
        }
        if (orderId == null) {
            throw new DomainValidationException("Order ID is required", 
                List.of("orderId cannot be null"));
        }
    }
}