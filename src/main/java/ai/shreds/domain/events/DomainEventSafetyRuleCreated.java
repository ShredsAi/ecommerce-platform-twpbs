package ai.shreds.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DomainEventSafetyRuleCreated {
    private final UUID ruleId;
    private final String skuId;
    private final String locationId;
    private final BigDecimal minQuantity;
    private final Instant occurredAt;

    public DomainEventSafetyRuleCreated(UUID ruleId,
                                       String skuId,
                                       String locationId,
                                       BigDecimal minQuantity,
                                       Instant occurredAt) {
        this.ruleId = ruleId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.minQuantity = minQuantity;
        this.occurredAt = occurredAt;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "DomainEventSafetyRuleCreated{" +
                "ruleId=" + ruleId +
                ", skuId='" + skuId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", minQuantity=" + minQuantity +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
