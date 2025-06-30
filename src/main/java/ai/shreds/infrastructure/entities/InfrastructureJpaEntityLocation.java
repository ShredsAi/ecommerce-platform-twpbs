package ai.shreds.infrastructure.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(
    name = "location",
    indexes = {
        @Index(name = "idx_location_type", columnList = "type"),
        @Index(name = "idx_location_active", columnList = "is_active"),
        @Index(name = "idx_location_name", columnList = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdAt"})
public class InfrastructureJpaEntityLocation {

    @Id
    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "location_id", length = 64, nullable = false)
    private String locationId;

    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Embedded
    private InfrastructureJpaEmbeddableAddress address;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (type == null || type.trim().isEmpty()) {
            type = "WAREHOUSE"; // Default type
        }
    }
    
    // Business methods
    public void activate() {
        this.isActive = true;
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public void updateAddress(InfrastructureJpaEmbeddableAddress address) {
        this.address = address;
        if (address != null) {
            address.validateAddress();
        }
    }
    
    // Validation method
    public void validateLocation() {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Location type cannot be null or empty");
        }
        if (address != null) {
            address.validateAddress();
        }
    }

    // Additional getter for MapStruct
    public boolean getIsActive() {
        return this.isActive;
    }
}