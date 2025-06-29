package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Index;
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
    name = "low_stock_alert",
    indexes = {
        @Index(name = "idx_low_stock_alert_sku_location", columnList = "sku_id,location_id"),
        @Index(name = "idx_low_stock_alert_status", columnList = "status"),
        @Index(name = "idx_low_stock_alert_created", columnList = "created_at"),
        @Index(name = "idx_low_stock_alert_rule", columnList = "rule_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"acknowledgedAt", "resolvedAt"})
public class InfrastructureJpaEntityLowStockAlert {

    @Id
    @Column(name = "alert_id", nullable = false, updatable = false)
    private UUID alertId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Current quantity cannot be negative")
    @Column(name = "current_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentQuantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Threshold cannot be negative")
    @Column(name = "threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal threshold;

    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    public void prePersist() {
        if (alertId == null) {
            alertId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING";
        }
    }
    
    // Business methods to change alert status
    public void acknowledge() {
        this.status = "ACKNOWLEDGED";
        this.acknowledgedAt = Instant.now();
    }
    
    public void resolve() {
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = Instant.now();
        }
    }
    
    // Validation method
    public void validateAlert() {
        if (currentQuantity == null || currentQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current quantity must be non-negative");
        }
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        if (!"PENDING".equals(status) && !"ACKNOWLEDGED".equals(status) && !"RESOLVED".equals(status)) {
            throw new IllegalArgumentException("Invalid alert status: " + status);
        }
    }
}
