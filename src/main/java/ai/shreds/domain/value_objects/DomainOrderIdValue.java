package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainValidationException;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class DomainOrderIdValue {
    private final UUID value;

    public DomainOrderIdValue(UUID value) {
        if (value == null) {
            throw new DomainValidationException("Order ID cannot be null", 
                List.of("value cannot be null"));
        }
        this.value = value;
    }

    public static DomainOrderIdValue generate() {
        return new DomainOrderIdValue(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderIdValue that = (DomainOrderIdValue) o;
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