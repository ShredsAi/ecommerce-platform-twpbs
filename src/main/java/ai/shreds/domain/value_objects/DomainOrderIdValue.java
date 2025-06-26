package ai.shreds.domain.value_objects;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class DomainOrderIdValue implements Serializable {

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID value;

    protected DomainOrderIdValue() {
        // for JPA
    }

    public DomainOrderIdValue(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value cannot be null");
        }
        this.value = value;
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderIdValue that = (DomainOrderIdValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}