package ai.shreds.domain.events;

import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DomainEventLowStockDetected {
    private final UUID alertId;
    private final String skuId;
    private final String locationId;
    private final BigDecimal currentQuantity;
    private final BigDecimal threshold;
    private final Instant occurredAt;

    public DomainEventLowStockDetected(UUID alertId,
                                       String skuId,
                                       String locationId,
                                       BigDecimal currentQuantity,
                                       BigDecimal threshold,
                                       Instant occurredAt) {
        this.alertId = alertId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.currentQuantity = currentQuantity;
        this.threshold = threshold;
        this.occurredAt = occurredAt;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public SharedLowStockAlertEvent toLowStockAlertEvent() {
        return new SharedLowStockAlertEvent(
                alertId.toString(),
                skuId,
                locationId,
                "LOW",
                currentQuantity,
                threshold
        );
    }

    @Override
    public String toString() {
        return "DomainEventLowStockDetected{" +
                "alertId=" + alertId +
                ", skuId='" + skuId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", currentQuantity=" + currentQuantity +
                ", threshold=" + threshold +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
