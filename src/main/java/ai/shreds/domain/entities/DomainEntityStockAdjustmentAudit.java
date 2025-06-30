package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainValueLedgerId;
import ai.shreds.domain.value_objects.DomainValueSkuId;
import ai.shreds.domain.value_objects.DomainValueLocationId;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DomainEntityStockAdjustmentAudit {
    private final UUID auditId;
    private final DomainValueLedgerId ledgerId;
    private final DomainValueSkuId skuId;
    private final DomainValueLocationId locationId;
    private final BigDecimal deltaQuantity;
    private final BigDecimal previousQuantity;
    private final BigDecimal newQuantity;
    private final SharedEnumAdjustmentReason reason;
    private final String source;
    private final Instant createdAt;
    private final String userId;

    public DomainEntityStockAdjustmentAudit(UUID auditId,
                                            DomainValueLedgerId ledgerId,
                                            DomainValueSkuId skuId,
                                            DomainValueLocationId locationId,
                                            BigDecimal deltaQuantity,
                                            BigDecimal previousQuantity,
                                            BigDecimal newQuantity,
                                            SharedEnumAdjustmentReason reason,
                                            String source,
                                            Instant createdAt,
                                            String userId) {
        this.auditId = auditId;
        this.ledgerId = ledgerId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.deltaQuantity = deltaQuantity;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason;
        this.source = source;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public static DomainEntityStockAdjustmentAudit create(
            DomainValueLedgerId ledgerId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            BigDecimal deltaQuantity,
            BigDecimal previousQuantity,
            BigDecimal newQuantity,
            SharedEnumAdjustmentReason reason,
            String source,
            String userId) {
        return new DomainEntityStockAdjustmentAudit(
                UUID.randomUUID(), ledgerId, skuId, locationId,
                deltaQuantity, previousQuantity, newQuantity,
                reason, source, Instant.now(), userId);
    }

    public static DomainEntityStockAdjustmentAudit reconstruct(
            UUID auditId,
            DomainValueLedgerId ledgerId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            BigDecimal deltaQuantity,
            BigDecimal previousQuantity,
            BigDecimal newQuantity,
            SharedEnumAdjustmentReason reason,
            String source,
            Instant createdAt,
            String userId) {
        return new DomainEntityStockAdjustmentAudit(
                auditId, ledgerId, skuId, locationId,
                deltaQuantity, previousQuantity, newQuantity,
                reason, source, createdAt, userId);
    }

    public UUID getAuditId() {
        return auditId;
    }

    public DomainValueLedgerId getLedgerId() {
        return ledgerId;
    }

    public DomainValueSkuId getSkuId() {
        return skuId;
    }

    public DomainValueLocationId getLocationId() {
        return locationId;
    }

    public BigDecimal getDeltaQuantity() {
        return deltaQuantity;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public SharedEnumAdjustmentReason getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUserId() {
        return userId;
    }
}
