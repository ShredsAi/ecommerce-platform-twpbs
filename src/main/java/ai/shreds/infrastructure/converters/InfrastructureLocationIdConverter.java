package ai.shreds.infrastructure.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ai.shreds.domain.value_objects.DomainValueLocationId;

@Converter(autoApply = true)
public class InfrastructureLocationIdConverter implements AttributeConverter<DomainValueLocationId, String> {

    @Override
    public String convertToDatabaseColumn(DomainValueLocationId locationId) {
        return locationId != null ? locationId.getValue() : null;
    }

    @Override
    public DomainValueLocationId convertToEntityAttribute(String dbData) {
        return dbData != null ? new DomainValueLocationId(dbData) : null;
    }
}