package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.value_objects.DomainValueProductId;
import ai.shreds.domain.value_objects.DomainValueSkuId;
import ai.shreds.domain.value_objects.DomainValueVendorSku;
import java.time.Instant;

public class DomainEntitySKU {
    private final DomainValueSkuId skuId;
    private DomainValueProductId productId;
    private DomainValueVendorSku vendorSku;
    private boolean isActive;
    private final Instant createdAt;
    private Instant updatedAt;

    private DomainEntitySKU(DomainValueSkuId skuId,
                            DomainValueProductId productId,
                            DomainValueVendorSku vendorSku,
                            boolean isActive,
                            Instant createdAt,
                            Instant updatedAt) {
        this.skuId = skuId;
        this.productId = productId;
        this.vendorSku = vendorSku;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DomainEntitySKU create(DomainValueSkuId skuId,
                                         DomainValueProductId productId,
                                         DomainValueVendorSku vendorSku) {
        Instant now = Instant.now();
        return new DomainEntitySKU(skuId, productId, vendorSku, true, now, now);
    }

    public static DomainEntitySKU reconstruct(DomainValueSkuId skuId,
                                              DomainValueProductId productId,
                                              DomainValueVendorSku vendorSku,
                                              boolean isActive,
                                              Instant createdAt,
                                              Instant updatedAt) {
        return new DomainEntitySKU(skuId, productId, vendorSku, isActive, createdAt, updatedAt);
    }

    public void activate() {
        if (!isActive) {
            isActive = true;
            updatedAt = Instant.now();
        }
    }

    public void deactivate() {
        if (isActive) {
            isActive = false;
            updatedAt = Instant.now();
        }
    }

    public void updateVendorSku(DomainValueVendorSku newVendorSku) {
        if (!isActive) {
            throw new DomainExceptionInvalidState("INACTIVE", "updateVendorSku");
        }
        this.vendorSku = newVendorSku;
        this.updatedAt = Instant.now();
    }

    public void updateProductId(DomainValueProductId newProductId) {
        if (!isActive) {
            throw new DomainExceptionInvalidState("INACTIVE", "updateProductId");
        }
        this.productId = newProductId;
        this.updatedAt = Instant.now();
    }

    public DomainValueSkuId getSkuId() {
        return skuId;
    }

    public DomainValueProductId getProductId() {
        return productId;
    }

    public DomainValueVendorSku getVendorSku() {
        return vendorSku;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}