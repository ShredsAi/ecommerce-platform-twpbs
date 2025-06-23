package ai.shreds.shared.value_objects;

import lombok.*;
import java.math.BigDecimal;
import ai.shreds.domain.value_objects.DomainMoneyValue;

/**
 * Shared representation of monetary value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedMoneyValue {
    private BigDecimal amount;
    private String currency;

    public DomainMoneyValue toDomainValue() {
        return new DomainMoneyValue(amount, currency);
    }

    public static SharedMoneyValue fromDomainValue(DomainMoneyValue value) {
        return SharedMoneyValue.builder()
                .amount(value.getAmount())
                .currency(value.getCurrency())
                .build();
    }
}