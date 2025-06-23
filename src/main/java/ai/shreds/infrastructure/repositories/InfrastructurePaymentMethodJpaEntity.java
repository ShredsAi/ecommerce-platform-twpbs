package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentMethodEntity;
import ai.shreds.domain.value_objects.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_methods")
public class InfrastructurePaymentMethodJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "details")
    @JdbcTypeCode(SqlTypes.JSON)
    private String details;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public static InfrastructurePaymentMethodJpaEntity fromDomainEntity(DomainPaymentMethodEntity domain) {
        InfrastructurePaymentMethodJpaEntity jpa = new InfrastructurePaymentMethodJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setCustomerId(domain.getCustomerId().getValue());
        jpa.setType(domain.getType().name());
        jpa.setIsDefault(domain.isDefault());
        jpa.setIsActive(domain.isActive());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        
        // Serialize payment method details to JSON
        if (domain.getDetails() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                jpa.setDetails(mapper.writeValueAsString(domain.getDetails()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                jpa.setDetails("{}");
            }
        }
        
        return jpa;
    }

    public DomainPaymentMethodEntity toDomainEntity() {
        DomainPaymentMethodDetailsValue details = null;
        
        if (this.details != null && !this.details.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> detailsMap = mapper.readValue(this.details, java.util.Map.class);
                details = createPaymentMethodDetails(DomainPaymentMethodTypeEnum.valueOf(this.type), detailsMap);
            } catch (Exception e) {
                // Create minimal details on parsing error
                details = new DomainPaymentMethodDetailsValue(
                    DomainPaymentMethodTypeEnum.valueOf(this.type), 
                    null, null, null
                );
            }
        }
        
        return new DomainPaymentMethodEntity(
            new DomainPaymentMethodIdValue(this.id),
            new DomainCustomerIdValue(this.customerId),
            DomainPaymentMethodTypeEnum.valueOf(this.type),
            details,
            this.isDefault,
            this.isActive,
            this.createdAt,
            this.updatedAt
        );
    }
    
    private DomainPaymentMethodDetailsValue createPaymentMethodDetails(DomainPaymentMethodTypeEnum type, java.util.Map<String, Object> detailsMap) {
        switch (type) {
            case CARD:
                return new DomainPaymentMethodDetailsValue(
                    type,
                    new DomainCardDetailsValue(
                        (String) detailsMap.get("last4"),
                        (String) detailsMap.get("brand"),
                        (Integer) detailsMap.get("expiryMonth"),
                        (Integer) detailsMap.get("expiryYear")
                    ),
                    null,
                    null
                );
            case BANK_ACCOUNT:
                return new DomainPaymentMethodDetailsValue(
                    type,
                    null,
                    new DomainBankAccountDetailsValue(
                        (String) detailsMap.get("last4"),
                        (String) detailsMap.get("bankName"),
                        (String) detailsMap.get("accountType")
                    ),
                    null
                );
            case DIGITAL_WALLET:
                return new DomainPaymentMethodDetailsValue(
                    type,
                    null,
                    null,
                    new DomainDigitalWalletDetailsValue(
                        (String) detailsMap.get("walletType"),
                        (String) detailsMap.get("email")
                    )
                );
            default:
                return new DomainPaymentMethodDetailsValue(type, null, null, null);
        }
    }

    // Explicit setters for boolean fields due to naming conventions
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
