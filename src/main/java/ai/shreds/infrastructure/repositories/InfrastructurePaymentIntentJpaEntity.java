package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.value_objects.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_intents")
public class InfrastructurePaymentIntentJpaEntity {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "processor_type", nullable = false, length = 16)
    private String processorType;

    @Column(name = "client_secret", nullable = false, length = 255)
    private String clientSecret;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static InfrastructurePaymentIntentJpaEntity fromDomainEntity(DomainPaymentIntentEntity domain) {
        InfrastructurePaymentIntentJpaEntity jpa = new InfrastructurePaymentIntentJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setOrderId(domain.getOrderId().getValue());
        jpa.setCustomerId(domain.getCustomerId().getValue());
        // Convert to cents to avoid floating point issues
        jpa.setAmountCents(domain.getAmount().getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        jpa.setCurrency(domain.getAmount().getCurrency());
        jpa.setStatus(domain.getStatus().name());
        jpa.setPaymentMethodId(domain.getPaymentMethodId() != null ? domain.getPaymentMethodId().getValue() : null);
        jpa.setProcessorType(domain.getProcessorType().name());
        jpa.setClientSecret(domain.getClientSecret());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        jpa.setVersion(domain.getVersion());
        return jpa;
    }

    public DomainPaymentIntentEntity toDomainEntity() {
        // Convert cents back to major currency units
        BigDecimal amount = BigDecimal.valueOf(this.amountCents).divide(BigDecimal.valueOf(100));
        DomainMoneyValue money = new DomainMoneyValue(amount, this.currency);
        
        return new DomainPaymentIntentEntity(
            new DomainPaymentIntentIdValue(this.id),
            new DomainOrderIdValue(this.orderId),
            new DomainCustomerIdValue(this.customerId),
            money,
            DomainPaymentStatusEnum.valueOf(this.status),
            this.paymentMethodId != null ? new DomainPaymentMethodIdValue(this.paymentMethodId) : null,
            DomainPaymentProcessorTypeEnum.valueOf(this.processorType),
            this.clientSecret,
            this.expiresAt,
            this.createdAt,
            this.updatedAt,
            this.version
        );
    }
}