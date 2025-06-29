package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Index;
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
    name = "sku",
    indexes = {
        @Index(name = "idx_sku_product_id", columnList = "product_id"),
        @Index(name = "idx_sku_vendor_sku", columnList = "vendor_sku"),
        @Index(name = "idx_sku_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdAt", "updatedAt"})
public class InfrastructureJpaEntitySKU {

    @Id
    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @Size(max = 64)
    @Column(name = "product_id", length = 64)
    private String productId;

    @Size(max = 64)
    @Column(name = "vendor_sku", length = 64)
    private String vendorSku;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    // Business methods
    public void activate() {
        this.isActive = true;
        this.onUpdate();
    }
    
    public void deactivate() {
        this.isActive = false;
        this.onUpdate();
    }
    
    public void updateVendorSku(String vendorSku) {
        this.vendorSku = vendorSku;
        this.onUpdate();
    }
    
    // Validation method
    public void validateSKU() {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
    }

    // Additional getter for MapStruct
    public boolean getIsActive() {
        return this.isActive;
    }
}