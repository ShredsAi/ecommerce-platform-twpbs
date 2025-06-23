package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentStatusUpdateEntity;
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
@Table(name = "payment_status_updates")
public class InfrastructurePaymentStatusUpdateJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "intent_id", nullable = false)
    private UUID intentId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "processor_type", nullable = false, length = 16)
    private String processorType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    public static InfrastructurePaymentStatusUpdateJpaEntity fromDomainEntity(DomainPaymentStatusUpdateEntity domain) {
        InfrastructurePaymentStatusUpdateJpaEntity jpa = new InfrastructurePaymentStatusUpdateJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPaymentId(domain.getPaymentId() != null ? domain.getPaymentId().getValue() : null);
        jpa.setIntentId(domain.getIntentId().getValue());
        jpa.setStatus(domain.getStatus().name());
        jpa.setProcessorType(domain.getProcessorType().name());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }

    public DomainPaymentStatusUpdateEntity toDomainEntity() {
        return new DomainPaymentStatusUpdateEntity(
            this.id,
            this.paymentId != null ? new DomainPaymentIdValue(this.paymentId) : null,
            new DomainPaymentIntentIdValue(this.intentId),
            DomainPaymentStatusEnum.valueOf(this.status),
            DomainPaymentProcessorTypeEnum.valueOf(this.processorType),
            this.updatedAt
        );
    }
}