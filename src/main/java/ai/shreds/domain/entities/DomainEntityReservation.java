package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.value_objects.*;
import java.time.Instant;
import java.time.Duration;

public class DomainEntityReservation {
    private final DomainValueReservationId reservationId;
    private final DomainValueSkuId skuId;
    private final DomainValueLocationId locationId;
    private final DomainValueQuantity quantity;
    private DomainEnumReservationStatus status;
    private Instant expiresAt;
    private final Instant createdAt;
    private final String reason;

    private DomainEntityReservation(
            DomainValueReservationId reservationId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity quantity,
            DomainEnumReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            String reason) {
        this.reservationId = reservationId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.reason = reason;
    }

    public static DomainEntityReservation create(
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity quantity,
            Instant expiresAt,
            String reason) {
        Instant now = Instant.now();
        if (expiresAt.isBefore(now)) {
            throw new DomainExceptionInvalidState("EXPIRED", "create reservation with past expiration");
        }
        return new DomainEntityReservation(
                DomainValueReservationId.create(),
                skuId,
                locationId,
                quantity,
                DomainEnumReservationStatus.PENDING,
                expiresAt,
                now,
                reason
        );
    }

    public static DomainEntityReservation reconstruct(
            DomainValueReservationId reservationId,
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            DomainValueQuantity quantity,
            DomainEnumReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            String reason) {
        return new DomainEntityReservation(reservationId, skuId, locationId, quantity, status, expiresAt, createdAt, reason);
    }

    public void confirm() {
        if (status != DomainEnumReservationStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "confirm");
        }
        status = DomainEnumReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != DomainEnumReservationStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "cancel");
        }
        status = DomainEnumReservationStatus.CANCELLED;
    }

    public void expire() {
        if (status != DomainEnumReservationStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "expire");
        }
        status = DomainEnumReservationStatus.EXPIRED;
    }

    public void extend(Duration duration) {
        if (status != DomainEnumReservationStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "extend");
        }
        Instant newExpiry = expiresAt.plus(duration);
        if (newExpiry.isBefore(Instant.now())) {
            throw new DomainExceptionInvalidState(expiresAt.toString(), "extend to a past time");
        }
        expiresAt = newExpiry;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public DomainValueReservationId getReservationId() {
        return reservationId;
    }

    public DomainValueSkuId getSkuId() {
        return skuId;
    }

    public DomainValueLocationId getLocationId() {
        return locationId;
    }

    public DomainValueQuantity getQuantity() {
        return quantity;
    }

    public DomainEnumReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getReason() {
        return reason;
    }
}
