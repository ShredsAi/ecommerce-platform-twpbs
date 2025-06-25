package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainValidationException;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class DomainQuantityValue {
    private final Integer value;

    public DomainQuantityValue(Integer value) {
        validate(value);
        this.value = value;
    }

    public void validate() {
        validate(this.value);
    }

    private void validate(Integer value) {
        if (value == null) {
            throw new DomainValidationException("Quantity cannot be null", 
                List.of("quantity must be provided"));
        }
        if (value <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero", 
                List.of("quantity must be > 0"));
        }
        if (value > 1000) {
            throw new DomainValidationException("Quantity cannot exceed 1000", 
                List.of("quantity must be <= 1000 to prevent abuse"));
        }
    }

    public DomainQuantityValue add(DomainQuantityValue other) {
        return new DomainQuantityValue(this.value + other.value);
    }

    public DomainQuantityValue subtract(DomainQuantityValue other) {
        int result = this.value - other.value;
        if (result <= 0) {
            throw new DomainValidationException("Quantity subtraction result must be positive", 
                List.of("result must be > 0"));
        }
        return new DomainQuantityValue(result);
    }

    public boolean isGreaterThan(DomainQuantityValue other) {
        return this.value > other.value;
    }

    public boolean isEqualTo(DomainQuantityValue other) {
        return this.value.equals(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainQuantityValue that = (DomainQuantityValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}