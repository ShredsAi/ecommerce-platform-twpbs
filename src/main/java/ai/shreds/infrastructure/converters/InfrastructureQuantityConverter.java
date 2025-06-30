package ai.shreds.infrastructure.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ai.shreds.domain.value_objects.DomainValueQuantity;
import ai.shreds.domain.value_objects.DomainEnumQuantityUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.util.Map;

@Converter(autoApply = true)
public class InfrastructureQuantityConverter implements AttributeConverter<DomainValueQuantity, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(DomainValueQuantity quantity) {
        if (quantity == null) {
            return null;
        }
        
        try {
            Map<String, Object> quantityData = Map.of(
                "value", quantity.getValue(),
                "unit", quantity.getUnit().name()
            );
            return objectMapper.writeValueAsString(quantityData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting quantity to database column", e);
        }
    }

    @Override
    public DomainValueQuantity convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new DomainValueQuantity(BigDecimal.ZERO, DomainEnumQuantityUnit.UNIT);
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> quantityData = objectMapper.readValue(dbData, Map.class);
            BigDecimal value = new BigDecimal(quantityData.get("value").toString());
            String unitName = quantityData.get("unit").toString();
            DomainEnumQuantityUnit unit = DomainEnumQuantityUnit.valueOf(unitName);
            return new DomainValueQuantity(value, unit);
        } catch (Exception e) {
            throw new RuntimeException("Error converting database data to quantity", e);
        }
    }
}