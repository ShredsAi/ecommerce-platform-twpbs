package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import ai.shreds.domain.value_objects.*;
import ai.shreds.domain.events.*;
import ai.shreds.shared.dtos.SharedStockLevelDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DomainEntityStockLedger {
    private final DomainValueLedgerId ledgerId;
    private final DomainValueSkuId skuId;
    private final DomainValueLocationId locationId;
    private DomainValueQuantity quantity;
    private DomainValueQuantity reserved;
    private Instant lastUpdated;
    private Integer version;

    private DomainEntityStockLedger(DomainValueLedgerId ledgerId,
                                    DomainValueSkuId skuId,
                                    DomainValueLocationId locationId,
                                    DomainValueQuantity quantity,
                                    DomainValueQuantity reserved,
                                    Instant lastUpdated,
                                    Integer version) {
        this.ledgerId = ledgerId;
        this.skuId = skuId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.reserved = reserved;
        this.lastUpdated = lastUpdated;
        this.version = version;
        validateInvariants();
    }

    public static DomainEntityStockLedger create(DomainValueSkuId skuId,
                                                 DomainValueLocationId locationId,
                                                 DomainValueQuantity initialQuantity) {
        return new DomainEntityStockLedger(DomainValueLedgerId.create(),
                skuId,
                locationId,
                initialQuantity,
                new DomainValueQuantity(BigDecimal.ZERO),
                Instant.now(),
                0);
    }

    public static DomainEntityStockLedger reconstruct(DomainValueLedgerId ledgerId,
                                                      DomainValueSkuId skuId,
                                                      DomainValueLocationId locationId,
                                                      DomainValueQuantity quantity,
                                                      DomainValueQuantity reserved,
                                                      Instant lastUpdated,
                                                      Integer version) {
        return new DomainEntityStockLedger(ledgerId, skuId, locationId, quantity, reserved, lastUpdated, version);
    }

    public DomainEventStockAdjusted adjustQuantity(DomainValueQuantityAdjustment adjustment) {
        DomainValueQuantity previous = this.quantity;
        DomainValueQuantity newQty = this.quantity.add(adjustment.getAdjustment());
        this.quantity = newQty;
        this.lastUpdated = Instant.now();
        this.version++;
        validateInvariants();
        return new DomainEventStockAdjusted(
                ledgerId.getValue(),
                skuId.getValue(),
                locationId.getValue(),
                previous.getValue(),
                newQty.getValue(),
                adjustment.getAdjustment().getValue(),
                adjustment.getReason(),
                lastUpdated
        );
    }

    public DomainEventReservationCreated reserveQuantity(DomainValueQuantity quantityToReserve) {
        if (calculateAvailable().compareTo(quantityToReserve) < 0) {
            throw new DomainExceptionInvariantViolation("Insufficient available stock for reservation");
        }
        this.reserved = this.reserved.add(quantityToReserve);
        this.lastUpdated = Instant.now();
        this.version++;
        return new DomainEventReservationCreated(
                UUID.randomUUID(),
                skuId.getValue(),
                locationId.getValue(),
                quantityToReserve.getValue(),
                lastUpdated.plusSeconds(3600), // assuming 1 hour expiry for demonstration
                lastUpdated
        );
    }

    public void releaseReservation(DomainValueQuantity quantityToRelease) {
        if (this.reserved.compareTo(quantityToRelease) < 0) {
            throw new DomainExceptionInvariantViolation("Cannot release more than reserved");
        }
        this.reserved = this.reserved.subtract(quantityToRelease);
        this.lastUpdated = Instant.now();
        this.version++;
    }

    public void allocateReserved(DomainValueQuantity quantityToAllocate) {
        if (this.reserved.compareTo(quantityToAllocate) < 0) {
            throw new DomainExceptionInvariantViolation("Cannot allocate more than reserved");
        }
        this.reserved = this.reserved.subtract(quantityToAllocate);
        this.quantity = this.quantity.subtract(quantityToAllocate);
        this.lastUpdated = Instant.now();
        this.version++;
        validateInvariants();
    }

    public DomainValueQuantity calculateAvailable() {
        return quantity.subtract(reserved);
    }

    public SharedStockLevelDTO toDTO() {
        return new SharedStockLevelDTO(
                skuId.getValue(),
                locationId.getValue(),
                quantity.getValue(),
                reserved.getValue(),
                calculateAvailable().getValue()
        );
    }

    private void validateInvariants() {
        if (quantity.isNegative()) {
            throw new DomainExceptionInvariantViolation("Quantity cannot be negative");
        }
        if (reserved.isNegative()) {
            throw new DomainExceptionInvariantViolation("Reserved quantity cannot be negative");
        }
        if (reserved.compareTo(quantity) > 0) {
            throw new DomainExceptionInvariantViolation("Reserved cannot exceed total quantity");
        }
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

    public DomainValueQuantity getQuantity() {
        return quantity;
    }

    public DomainValueQuantity getReserved() {
        return reserved;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public Integer getVersion() {
        return version;
    }
}
