package ai.shreds.domain.events;

import ai.shreds.domain.entities.DomainEntityStockLedger;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DomainEventStockAdjusted {
    private final UUID ledgerId;
    private final String skuId;
    private final String locationId;
    private final BigDecimal previousQuantity;
    private final BigDecimal newQuantity;
    private final BigDecimal adjustment;
    private final SharedEnumAdjustmentReason reason;
    private final Instant occurredAt;

    public DomainEventStockAdjusted(UUID ledgerId, String skuId, String locationId,
                                    BigDecimal previousQuantity, BigDecimal newQuantity,
                                    BigDecimal adjustment, SharedEnumAdjustmentReason reason, Instant occurredAt) {
        this.ledgerId = ledgerId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.adjustment = adjustment;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getLedgerId() {
        return ledgerId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public BigDecimal getAdjustment() {
        return adjustment;
    }

    public SharedEnumAdjustmentReason getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    // Transforms this domain event to a shared DTO for external publication
    public SharedInventoryChangedEvent toInventoryChangedEvent() {
        return new SharedInventoryChangedEvent(
                "StockAdjusted",
                skuId,
                locationId,
                previousQuantity,
                newQuantity,
                "DOMAIN"
        );
    }

    @Override
    public String toString() {
        return "DomainEventStockAdjusted{" +
                "ledgerId=" + ledgerId +
                ", skuId='" + skuId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", previousQuantity=" + previousQuantity +
                ", newQuantity=" + newQuantity +
                ", adjustment=" + adjustment +
                ", reason=" + reason +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
