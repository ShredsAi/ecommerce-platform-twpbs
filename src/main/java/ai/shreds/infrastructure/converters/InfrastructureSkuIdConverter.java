package ai.shreds.infrastructure.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ai.shreds.domain.value_objects.DomainValueSkuId;

@Converter(autoApply = true)
public class InfrastructureSkuIdConverter implements AttributeConverter<DomainValueSkuId, String> {

    @Override
    public String convertToDatabaseColumn(DomainValueSkuId skuId) {
        return skuId != null ? skuId.getValue() : null;
    }

    @Override
    public DomainValueSkuId convertToEntityAttribute(String dbData) {
        return dbData != null ? new DomainValueSkuId(dbData) : null;
    }
}