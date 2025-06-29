package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Index;
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
    name = "stock_adjustment_audit",
    indexes = {
        @Index(name = "idx_audit_ledger", columnList = "ledger_id"),
        @Index(name = "idx_audit_sku_location", columnList = "sku_id,location_id"),
        @Index(name = "idx_audit_created", columnList = "created_at"),
        @Index(name = "idx_audit_reason", columnList = "reason"),
        @Index(name = "idx_audit_source", columnList = "source")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"userId"})
public class InfrastructureJpaEntityStockAdjustmentAudit {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @NotNull
    @Column(name = "ledger_id", nullable = false)
    private UUID ledgerId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @NotNull
    @Column(name = "delta_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal deltaQuantity;

    @NotNull
    @Column(name = "previous_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousQuantity;

    @NotNull
    @Column(name = "new_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal newQuantity;

    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "reason", nullable = false, length = 32)
    private String reason;

    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "source", nullable = false, length = 32)
    private String source = "INTERNAL";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Size(max = 64)
    @Column(name = "user_id", length = 64)
    private String userId;

    @PrePersist
    public void prePersist() {
        if (auditId == null) {
            auditId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (source == null || source.trim().isEmpty()) {
            source = "INTERNAL";
        }
    }
    
    // Validation method
    public void validateAudit() {
        if (ledgerId == null) {
            throw new IllegalArgumentException("Ledger ID cannot be null");
        }
        if (deltaQuantity == null) {
            throw new IllegalArgumentException("Delta quantity cannot be null");
        }
        if (previousQuantity == null || previousQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Previous quantity must be non-negative");
        }
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("New quantity must be non-negative");
        }
        // Validate the calculation: newQuantity = previousQuantity + deltaQuantity
        BigDecimal expectedNewQuantity = previousQuantity.add(deltaQuantity);
        if (newQuantity.compareTo(expectedNewQuantity) != 0) {
            throw new IllegalArgumentException("Quantity calculations are inconsistent");
        }
    }
}
