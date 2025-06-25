package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainValidationException;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class DomainCustomerIdValue {
    private final String value;

    public DomainCustomerIdValue(String value) {
        validate(value);
        this.value = value;
    }

    public void validate() {
        validate(this.value);
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException("Customer ID cannot be null or empty", 
                List.of("customerId must be provided"));
        }
        if (value.length() > 50) {
            throw new DomainValidationException("Customer ID cannot exceed 50 characters", 
                List.of("customerId length must be <= 50"));
        }
        if (!value.matches("^[a-zA-Z0-9_-]+$")) {
            throw new DomainValidationException("Customer ID contains invalid characters", 
                List.of("customerId must contain only alphanumeric characters, underscores and hyphens"));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCustomerIdValue that = (DomainCustomerIdValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}