package ai.shreds.domain.events;

import java.time.Instant;
import java.util.UUID;

public class DomainEventReservationCreated {
    private final UUID reservationId;
    private final String skuId;
    private final String locationId;
    private final java.math.BigDecimal quantity;
    private final Instant expiresAt;
    private final Instant occurredAt;

    public DomainEventReservationCreated(UUID reservationId,
                                         String skuId,
                                         String locationId,
                                         java.math.BigDecimal quantity,
                                         Instant expiresAt,
                                         Instant occurredAt) {
        this.reservationId = reservationId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.occurredAt = occurredAt;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public java.math.BigDecimal getQuantity() {
        return quantity;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "DomainEventReservationCreated{" +
                "reservationId=" + reservationId +
                ", skuId='" + skuId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", quantity=" + quantity +
                ", expiresAt=" + expiresAt +
                ", occurredAt=" + occurredAt +
                '}';
    }
}