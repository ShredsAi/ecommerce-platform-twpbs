package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
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
    name = "stock_ledger", 
    indexes = {
        @Index(name = "idx_stock_ledger_sku_location", columnList = "sku_id,location_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_ledger_sku_location", columnNames = {"sku_id", "location_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"version"})
public class InfrastructureJpaEntityStockLedger {

    @Id
    @Column(name = "ledger_id", nullable = false, updatable = false)
    private UUID ledgerId;

    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Quantity cannot be negative")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Reserved quantity cannot be negative")
    @Column(name = "reserved", nullable = false, precision = 19, scale = 4)
    private BigDecimal reserved;

    // This field is mapped to a generated column in the database
    // In PostgreSQL, it will be created as: available NUMERIC(19,4) GENERATED ALWAYS AS (quantity - reserved) STORED
    @Column(name = "available", nullable = false, insertable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal available;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (ledgerId == null) {
            ledgerId = UUID.randomUUID();
        }
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        if (reserved == null) {
            reserved = BigDecimal.ZERO;
        }
        lastUpdated = now;
        
        // Calculate available for in-memory purposes (database has a generated column)
        available = quantity.subtract(reserved);
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = Instant.now();
        
        // Calculate available for in-memory purposes (database has a generated column)
        available = quantity.subtract(reserved);
    }
    
    // Helper method to validate business rules
    public void validateInvariants() {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (reserved.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative");
        }
        if (reserved.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Reserved quantity cannot exceed total quantity");
        }
    }
}
