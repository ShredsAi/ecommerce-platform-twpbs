package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentTokenEntity;
import ai.shreds.domain.value_objects.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_tokens")
public class InfrastructurePaymentTokenJpaEntity {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_method_id", nullable = false, unique = true, updatable = false)
    private UUID paymentMethodId;

    @Column(name = "processor_token", nullable = false, columnDefinition = "TEXT")
    private String processorToken;

    @Column(name = "processor_type", nullable = false, length = 16)
    private String processorType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public static InfrastructurePaymentTokenJpaEntity fromDomainEntity(DomainPaymentTokenEntity domain) {
        InfrastructurePaymentTokenJpaEntity jpa = new InfrastructurePaymentTokenJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPaymentMethodId(domain.getPaymentMethodId().getValue());
        jpa.setProcessorToken(domain.getProcessorToken());
        jpa.setProcessorType(domain.getProcessorType().name());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }

    public DomainPaymentTokenEntity toDomainEntity() {
        return new DomainPaymentTokenEntity(
            this.id,
            new DomainPaymentMethodIdValue(this.paymentMethodId),
            this.processorToken,
            DomainPaymentProcessorTypeEnum.valueOf(this.processorType),
            this.expiresAt,
            this.createdAt,
            this.updatedAt
        );
    }
}