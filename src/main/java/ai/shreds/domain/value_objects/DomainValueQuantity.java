package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvalidQuantity;
import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class DomainValueQuantity implements Comparable<DomainValueQuantity> {
    private final BigDecimal value;
    private final DomainEnumQuantityUnit unit;

    public DomainValueQuantity(BigDecimal value, DomainEnumQuantityUnit unit) {
        validate(value);
        this.value = value.setScale(4, RoundingMode.HALF_UP);
        this.unit = Objects.requireNonNull(unit, "Unit cannot be null");
    }

    public DomainValueQuantity(BigDecimal value) {
        this(value, DomainEnumQuantityUnit.UNIT);
    }

    private void validate(BigDecimal value) {
        if (value == null) {
            throw new DomainExceptionInvariantViolation("Quantity value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainExceptionInvalidQuantity(value, "Quantity cannot be negative");
        }
    }

    public BigDecimal getValue() {
        return value;
    }

    public DomainEnumQuantityUnit getUnit() {
        return unit;
    }

    public DomainValueQuantity add(DomainValueQuantity other) {
        ensureSameUnit(other);
        return new DomainValueQuantity(this.value.add(other.value), this.unit);
    }

    public DomainValueQuantity subtract(DomainValueQuantity other) {
        ensureSameUnit(other);
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainExceptionInvalidQuantity(result, "Resulting quantity cannot be negative");
        }
        return new DomainValueQuantity(result, this.unit);
    }

    public DomainValueQuantity multiply(BigDecimal factor) {
        return new DomainValueQuantity(this.value.multiply(factor), this.unit);
    }

    public boolean isNegative() {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public int compareTo(DomainValueQuantity other) {
        ensureSameUnit(other);
        return this.value.compareTo(other.value);
    }

    private void ensureSameUnit(DomainValueQuantity other) {
        if (this.unit != other.unit) {
            throw new DomainExceptionInvariantViolation(
                    "Cannot operate on quantities with different units: " + this.unit + " vs " + other.unit);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueQuantity)) return false;
        DomainValueQuantity that = (DomainValueQuantity) o;
        return value.equals(that.value) && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    @Override
    public String toString() {
        return value + " " + unit;
    }
}