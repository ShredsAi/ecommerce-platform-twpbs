package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class DomainValueRuleId {
    private final UUID value;

    public DomainValueRuleId(UUID value) {
        this.value = Objects.requireNonNull(value, "RuleId cannot be null");
    }

    public static DomainValueRuleId create() {
        return new DomainValueRuleId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueRuleId)) return false;
        DomainValueRuleId that = (DomainValueRuleId) o;
        return value.equals(that.value);
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