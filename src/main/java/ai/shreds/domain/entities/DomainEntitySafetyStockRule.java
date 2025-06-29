package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedSafetyRuleResponseDTO;
import java.time.Instant;

public class DomainEntitySafetyStockRule {
    private final DomainValueRuleId ruleId;
    private final DomainValueSkuId skuId;
    private final DomainValueLocationId locationId;
    private DomainValueQuantity minQuantity;
    private boolean isActive;
    private final Instant createdAt;
    private Instant updatedAt;

    private DomainEntitySafetyStockRule(
            DomainValueRuleId ruleId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity minQuantity,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
        this.ruleId = ruleId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.minQuantity = minQuantity;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DomainEntitySafetyStockRule create(
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity minQuantity) {
        Instant now = Instant.now();
        return new DomainEntitySafetyStockRule(
                DomainValueRuleId.create(),
                skuId,
                locationId,
                minQuantity,
                true,
                now,
                now
        );
    }

    public static DomainEntitySafetyStockRule reconstruct(
            DomainValueRuleId ruleId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity minQuantity,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
        return new DomainEntitySafetyStockRule(ruleId, skuId, locationId, minQuantity, isActive, createdAt, updatedAt);
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

    public void updateThreshold(DomainValueQuantity newMinQuantity) {
        this.minQuantity = newMinQuantity;
        this.updatedAt = Instant.now();
    }

    public SharedSafetyRuleResponseDTO toDTO() {
        return new SharedSafetyRuleResponseDTO(
                ruleId.getValue().toString(),
                skuId.getValue(),
                locationId.getValue(),
                minQuantity.getValue(),
                isActive
        );
    }

    public DomainValueRuleId getRuleId() {
        return ruleId;
    }

    public DomainValueSkuId getSkuId() {
        return skuId;
    }

    public DomainValueLocationId getLocationId() {
        return locationId;
    }

    public DomainValueQuantity getMinQuantity() {
        return minQuantity;
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
