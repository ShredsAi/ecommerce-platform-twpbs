package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.enums.SharedEnumAlertStatus;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import java.time.Instant;

public class DomainEntityLowStockAlert {
    private final DomainValueAlertId alertId;
    private final DomainValueSkuId skuId;
    private final DomainValueLocationId locationId;
    private final DomainValueRuleId ruleId;
    private final DomainValueQuantity currentQuantity;
    private final DomainValueQuantity threshold;
    private SharedEnumAlertStatus status;
    private final Instant createdAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;

    private DomainEntityLowStockAlert(
            DomainValueAlertId alertId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueRuleId ruleId,
            DomainValueQuantity currentQuantity,
            DomainValueQuantity threshold,
            SharedEnumAlertStatus status,
            Instant createdAt,
            Instant acknowledgedAt,
            Instant resolvedAt) {
        this.alertId = alertId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.ruleId = ruleId;
        this.currentQuantity = currentQuantity;
        this.threshold = threshold;
        this.status = status;
        this.createdAt = createdAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
    }

    public static DomainEntityLowStockAlert create(
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueRuleId ruleId,
            DomainValueQuantity currentQuantity,
            DomainValueQuantity threshold) {
        Instant now = Instant.now();
        return new DomainEntityLowStockAlert(
                DomainValueAlertId.create(),
                skuId,
                locationId,
                ruleId,
                currentQuantity,
                threshold,
                SharedEnumAlertStatus.PENDING,
                now,
                null,
                null
        );
    }

    public static DomainEntityLowStockAlert reconstruct(
            DomainValueAlertId alertId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueRuleId ruleId,
            DomainValueQuantity currentQuantity,
            DomainValueQuantity threshold,
            SharedEnumAlertStatus status,
            Instant createdAt,
            Instant acknowledgedAt,
            Instant resolvedAt) {
        return new DomainEntityLowStockAlert(alertId, skuId, locationId, ruleId, currentQuantity, threshold, status, createdAt, acknowledgedAt, resolvedAt);
    }

    public void acknowledge() {
        if (status != SharedEnumAlertStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "acknowledge");
        }
        status = SharedEnumAlertStatus.ACKNOWLEDGED;
        acknowledgedAt = Instant.now();
    }

    public void resolve() {
        if (status != SharedEnumAlertStatus.ACKNOWLEDGED) {
            throw new DomainExceptionInvalidState(status.name(), "resolve");
        }
        status = SharedEnumAlertStatus.RESOLVED;
        resolvedAt = Instant.now();
    }

    public void escalate() {
        if (status != SharedEnumAlertStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "escalate");
        }
        // escalation logic handled externally
    }

    public SharedLowStockAlertEvent toEvent() {
        return new SharedLowStockAlertEvent(
                alertId.getValue().toString(),
                skuId.getValue(),
                locationId.getValue(),
                "LOW", // alertLevel constant for low stock
                currentQuantity.getValue(),
                threshold.getValue()
        );
    }

    public DomainValueAlertId getAlertId() {
        return alertId;
    }

    public DomainValueSkuId getSkuId() {
        return skuId;
    }

    public DomainValueLocationId getLocationId() {
        return locationId;
    }

    public DomainValueRuleId getRuleId() {
        return ruleId;
    }

    public DomainValueQuantity getCurrentQuantity() {
        return currentQuantity;
    }

    public DomainValueQuantity getThreshold() {
        return threshold;
    }

    public SharedEnumAlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
