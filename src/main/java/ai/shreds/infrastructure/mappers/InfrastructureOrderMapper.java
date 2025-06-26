package ai.shreds.infrastructure.mappers;

import org.mapstruct.Mapper;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Duration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.Map;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.infrastructure.repositories.InfrastructureOrderJPAEntity;
import ai.shreds.infrastructure.repositories.InfrastructureOrderItemJPAEntity;
import ai.shreds.infrastructure.repositories.InfrastructureSagaStateJPAEntity;
import ai.shreds.infrastructure.repositories.InfrastructureOrderEventJPAEntity;
import ai.shreds.infrastructure.repositories.InfrastructurePaymentDetailsJPAEntity;
import ai.shreds.infrastructure.repositories.InfrastructureShippingDetailsJPAEntity;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainProductIdValue;
import ai.shreds.domain.value_objects.DomainQuantityValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;

@Mapper(componentModel = "spring")
public interface InfrastructureOrderMapper {

    DomainOrderEntity toDomainEntity(InfrastructureOrderJPAEntity jpa);

    InfrastructureOrderJPAEntity toJPAEntity(DomainOrderEntity domain);

    DomainOrderItemEntity toDomainOrderItemEntity(InfrastructureOrderItemJPAEntity jpaItem);

    InfrastructureOrderItemJPAEntity toJPAOrderItemEntity(DomainOrderItemEntity domainItem);

    DomainSagaStateEntity toDomainSagaState(InfrastructureSagaStateJPAEntity jpa);

    InfrastructureSagaStateJPAEntity toJPASagaState(DomainSagaStateEntity domain);

    DomainOrderEventEntity toDomainOrderEvent(InfrastructureOrderEventJPAEntity jpa);

    InfrastructureOrderEventJPAEntity toJPAOrderEvent(DomainOrderEventEntity domain);

    DomainPaymentDetailsEntity toDomainPaymentDetails(InfrastructurePaymentDetailsJPAEntity jpa);

    InfrastructurePaymentDetailsJPAEntity toJPAPaymentDetails(DomainPaymentDetailsEntity domain);

    DomainShippingDetailsEntity toDomainShippingDetails(InfrastructureShippingDetailsJPAEntity jpa);

    InfrastructureShippingDetailsJPAEntity toJPAShippingDetails(DomainShippingDetailsEntity domain);

    List<DomainOrderEntity> toDomainEntityList(List<InfrastructureOrderJPAEntity> jpas);

    List<InfrastructureOrderJPAEntity> toJPAEntityList(List<DomainOrderEntity> domains);

    // Custom mapping methods for value objects and primitives
    default UUID map(DomainOrderIdValue value) {
        return value == null ? null : value.getValue();
    }

    default DomainOrderIdValue map(UUID value) {
        return value == null ? null : new DomainOrderIdValue(value);
    }

    default String map(DomainCustomerIdValue value) {
        return value == null ? null : value.getValue();
    }

    default DomainCustomerIdValue mapToDomainCustomerId(String value) {
        return value == null ? null : new DomainCustomerIdValue(value);
    }

    default String map(DomainProductIdValue value) {
        return value == null ? null : value.getValue();
    }

    default DomainProductIdValue mapToDomainProductId(String value) {
        return value == null ? null : new DomainProductIdValue(value);
    }

    default Integer map(DomainQuantityValue value) {
        return value == null ? null : value.getValue();
    }

    default DomainQuantityValue mapToDomainQuantity(Integer value) {
        return value == null ? null : new DomainQuantityValue(value);
    }

    default BigDecimal map(SharedMoneyValue value) {
        return value == null ? null : value.getValue();
    }

    default SharedMoneyValue mapSharedMoneyValue(BigDecimal value) {
        return value == null ? null : new SharedMoneyValue(value, null);
    }

    // Mapping for Duration <-> Long
    default Long map(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    default Duration mapToDuration(Long millis) {
        return millis == null ? null : Duration.ofMillis(millis);
    }

    // Mapping for eventData: String <-> Map<String, String>
    default Map<String, String> mapEventData(String json) {
        if (json == null) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(json,
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse eventData JSON", e);
        }
    }

    default String mapMapToEventData(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write eventData JSON", e);
        }
    }
}