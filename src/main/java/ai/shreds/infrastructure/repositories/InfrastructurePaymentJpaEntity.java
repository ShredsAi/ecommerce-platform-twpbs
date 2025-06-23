package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.value_objects.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class InfrastructurePaymentJpaEntity {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_intent_id", nullable = false, unique = true, updatable = false)
    private UUID paymentIntentId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "processor_type", nullable = false, length = 16)
    private String processorType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "processor_response", columnDefinition = "jsonb")
    private Map<String, Object> processorResponse;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

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

    public static InfrastructurePaymentJpaEntity fromDomainEntity(DomainPaymentEntity domain) {
        InfrastructurePaymentJpaEntity jpa = new InfrastructurePaymentJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setPaymentIntentId(domain.getPaymentIntentId().getValue());
        // Convert to cents to avoid floating point issues
        jpa.setAmountCents(domain.getAmount().getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        jpa.setCurrency(domain.getAmount().getCurrency());
        jpa.setStatus(domain.getStatus().name());
        jpa.setProcessorType(domain.getProcessorType().name());
        
        // Create processor response map
        if (domain.getProcessorResponse() != null) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("processorId", domain.getProcessorResponse().getProcessorId());
            responseMap.put("responseCode", domain.getProcessorResponse().getResponseCode());
            responseMap.put("responseMessage", domain.getProcessorResponse().getResponseMessage());
            responseMap.put("rawResponse", domain.getProcessorResponse().getRawResponse());
            if (domain.getProcessorResponse().getMetadata() != null) {
                responseMap.put("metadata", domain.getProcessorResponse().getMetadata());
            }
            jpa.setProcessorResponse(responseMap);
        }
        
        jpa.setProcessedAt(domain.getProcessedAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        jpa.setVersion(domain.getVersion());
        return jpa;
    }

    public DomainPaymentEntity toDomainEntity() {
        // Parse processor response from Map
        DomainProcessorResponseValue processorResponseValue = null;
        if (this.processorResponse != null) {
            Map<String, Object> metadata = null;
            if (this.processorResponse.containsKey("metadata") && this.processorResponse.get("metadata") instanceof Map) {
                metadata = (Map<String, Object>) this.processorResponse.get("metadata");
            }
            
            processorResponseValue = new DomainProcessorResponseValue(
                (String) this.processorResponse.get("processorId"),
                (String) this.processorResponse.get("responseCode"),
                (String) this.processorResponse.get("responseMessage"),
                (String) this.processorResponse.get("rawResponse"),
                metadata
            );
        }
        
        // Convert cents back to major currency units
        BigDecimal amount = BigDecimal.valueOf(this.amountCents).divide(BigDecimal.valueOf(100));
        
        // Use the static factory method reconstruct() instead of private constructor
        return DomainPaymentEntity.reconstruct(
            new DomainPaymentIdValue(this.id),
            new DomainPaymentIntentIdValue(this.paymentIntentId),
            new DomainMoneyValue(amount, this.currency),
            DomainPaymentStatusEnum.valueOf(this.status),
            DomainPaymentProcessorTypeEnum.valueOf(this.processorType),
            processorResponseValue,
            this.processedAt,
            this.createdAt,
            this.updatedAt,
            this.version
        );
    }
}