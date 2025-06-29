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
import java.time.Duration;
import java.util.UUID;

@Entity
@Table(
    name = "reservation",
    indexes = {
        @Index(name = "idx_reservation_sku_location", columnList = "sku_id,location_id"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expires", columnList = "expires_at"),
        @Index(name = "idx_reservation_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"reason"})
public class InfrastructureJpaEntityReservation {

    @Id
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (reservationId == null) {
            reservationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING";
        }
        // Set default expiration if not provided (24 hours from now)
        if (expiresAt == null) {
            expiresAt = now.plus(Duration.ofHours(24));
        }
    }
    
    // Business methods for status management
    public void confirm() {
        this.status = "CONFIRMED";
    }
    
    public void cancel() {
        this.status = "CANCELLED";
    }
    
    public void expire() {
        this.status = "EXPIRED";
    }
    
    public void extend(Duration duration) {
        this.expiresAt = this.expiresAt.plus(duration);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    // Validation method
    public void validateReservation() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expiration time must be in the future");
        }
        if (!"PENDING".equals(status) && !"CONFIRMED".equals(status) && 
            !"CANCELLED".equals(status) && !"EXPIRED".equals(status)) {
            throw new IllegalArgumentException("Invalid reservation status: " + status);
        }
    }
}
