package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import ai.shreds.domain.value_objects.DomainMoneyValue;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedMoneyDTO {
    private BigDecimal amount;
    private String currency;

    public DomainMoneyValue toDomainValue() {
        DomainMoneyValue value = new DomainMoneyValue(amount, currency);
        value.validate();
        return value;
    }

    public static SharedMoneyDTO fromDomainValue(DomainMoneyValue money) {
        return new SharedMoneyDTO(money.getAmount(), money.getCurrency());
    }
}