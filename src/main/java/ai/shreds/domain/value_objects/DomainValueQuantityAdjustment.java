package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;
import java.util.Objects;

public final class DomainValueQuantityAdjustment {
    private final DomainValueQuantity adjustment;
    private final SharedEnumAdjustmentReason reason;
    private final String notes;

    public DomainValueQuantityAdjustment(DomainValueQuantity adjustment,
                                         SharedEnumAdjustmentReason reason,
                                         String notes) {
        if (adjustment == null) {
            throw new DomainExceptionInvariantViolation("Adjustment cannot be null");
        }
        if (reason == null) {
            throw new DomainExceptionInvariantViolation("Adjustment reason cannot be null");
        }
        this.adjustment = adjustment;
        this.reason = reason;
        this.notes = notes == null ? "" : notes;
    }

    public DomainValueQuantity getAdjustment() {
        return adjustment;
    }

    public SharedEnumAdjustmentReason getReason() {
        return reason;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueQuantityAdjustment)) return false;
        DomainValueQuantityAdjustment that = (DomainValueQuantityAdjustment) o;
        return adjustment.equals(that.adjustment) && reason == that.reason && notes.equals(that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adjustment, reason, notes);
    }

    @Override
    public String toString() {
        return adjustment + " (" + reason + ")" + (notes.isEmpty() ? "" : ": " + notes);
    }
}