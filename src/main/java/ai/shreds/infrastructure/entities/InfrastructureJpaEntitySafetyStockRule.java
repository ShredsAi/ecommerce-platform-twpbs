package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "safety_stock_rule",
    indexes = {
        @Index(name = "idx_safety_stock_rule_sku_location", columnList = "sku_id,location_id"),
        @Index(name = "idx_safety_stock_rule_active", columnList = "is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_safety_stock_rule_sku_location", columnNames = {"sku_id", "location_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdAt", "updatedAt"})
public class InfrastructureJpaEntitySafetyStockRule {

    @Id
    @Column(name = "rule_id", nullable = false, updatable = false)
    private UUID ruleId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum quantity cannot be negative")
    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (ruleId == null) {
            ruleId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
    
    // Business validation method
    public void validateRule() {
        if (minQuantity == null || minQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum quantity must be non-negative");
        }
    }

    // Additional getter for MapStruct
    public boolean getIsActive() {
        return this.isActive;
    }
}