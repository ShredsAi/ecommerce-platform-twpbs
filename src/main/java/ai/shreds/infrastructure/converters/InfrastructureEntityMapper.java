package ai.shreds.infrastructure.converters;

import org.mapstruct.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import ai.shreds.domain.entities.*;
import ai.shreds.domain.value_objects.*;
import ai.shreds.infrastructure.entities.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InfrastructureEntityMapper {

    // Stock Ledger mappings
    @Mapping(target = "ledgerId", expression = "java(ledger.getLedgerId() != null ? ledger.getLedgerId().getValue() : null)")
    @Mapping(target = "skuId", expression = "java(ledger.getSkuId() != null ? ledger.getSkuId().getValue() : null)")
    @Mapping(target = "locationId", expression = "java(ledger.getLocationId() != null ? ledger.getLocationId().getValue() : null)")
    @Mapping(target = "quantity", expression = "java(ledger.getQuantity() != null ? ledger.getQuantity().getValue() : null)")
    @Mapping(target = "reserved", expression = "java(ledger.getReserved() != null ? ledger.getReserved().getValue() : null)")
    @Mapping(target = "available", expression = "java(ledger.calculateAvailable() != null ? ledger.calculateAvailable().getValue() : null)")
    @Mapping(target = "lastUpdated", source = "lastUpdated")
    @Mapping(target = "version", source = "version")
    InfrastructureJpaEntityStockLedger toJpaEntity(DomainEntityStockLedger ledger);

    default DomainEntityStockLedger toDomainEntity(InfrastructureJpaEntityStockLedger jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityStockLedger.reconstruct(
            new DomainValueLedgerId(jpaEntity.getLedgerId()),
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueLocationId(jpaEntity.getLocationId()),
            new DomainValueQuantity(jpaEntity.getQuantity(), DomainEnumQuantityUnit.UNIT),
            new DomainValueQuantity(jpaEntity.getReserved(), DomainEnumQuantityUnit.UNIT),
            jpaEntity.getLastUpdated(),
            jpaEntity.getVersion()
        );
    }

    // Safety Stock Rule mappings
    @Mapping(target = "ruleId", expression = "java(rule.getRuleId() != null ? rule.getRuleId().getValue() : null)")
    @Mapping(target = "skuId", expression = "java(rule.getSkuId() != null ? rule.getSkuId().getValue() : null)")
    @Mapping(target = "locationId", expression = "java(rule.getLocationId() != null ? rule.getLocationId().getValue() : null)")
    @Mapping(target = "minQuantity", expression = "java(rule.getMinQuantity() != null ? rule.getMinQuantity().getValue() : null)")
    @Mapping(target = "active", expression = "java(rule.isActive())") // Revert back to 'active' for MapStruct bean property mapping
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    InfrastructureJpaEntitySafetyStockRule toJpaEntity(DomainEntitySafetyStockRule rule);

    default DomainEntitySafetyStockRule toDomainEntity(InfrastructureJpaEntitySafetyStockRule jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntitySafetyStockRule.reconstruct(
            new DomainValueRuleId(jpaEntity.getRuleId()),
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueLocationId(jpaEntity.getLocationId()),
            new DomainValueQuantity(jpaEntity.getMinQuantity(), DomainEnumQuantityUnit.UNIT),
            jpaEntity.getIsActive(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getUpdatedAt()
        );
    }

    // Low Stock Alert mappings
    @Mapping(target = "alertId", expression = "java(alert.getAlertId() != null ? alert.getAlertId().getValue() : null)")
    @Mapping(target = "skuId", expression = "java(alert.getSkuId() != null ? alert.getSkuId().getValue() : null)")
    @Mapping(target = "locationId", expression = "java(alert.getLocationId() != null ? alert.getLocationId().getValue() : null)")
    @Mapping(target = "ruleId", expression = "java(alert.getRuleId() != null ? alert.getRuleId().getValue() : null)")
    @Mapping(target = "currentQuantity", expression = "java(alert.getCurrentQuantity() != null ? alert.getCurrentQuantity().getValue() : null)")
    @Mapping(target = "threshold", expression = "java(alert.getThreshold() != null ? alert.getThreshold().getValue() : null)")
    @Mapping(target = "status", expression = "java(alert.getStatus() != null ? alert.getStatus().name() : null)")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "acknowledgedAt", source = "acknowledgedAt")
    @Mapping(target = "resolvedAt", source = "resolvedAt")
    InfrastructureJpaEntityLowStockAlert toJpaEntity(DomainEntityLowStockAlert alert);

    default DomainEntityLowStockAlert toDomainEntity(InfrastructureJpaEntityLowStockAlert jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityLowStockAlert.reconstruct(
            new DomainValueAlertId(jpaEntity.getAlertId()),
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueLocationId(jpaEntity.getLocationId()),
            new DomainValueRuleId(jpaEntity.getRuleId()),
            new DomainValueQuantity(jpaEntity.getCurrentQuantity(), DomainEnumQuantityUnit.UNIT),
            new DomainValueQuantity(jpaEntity.getThreshold(), DomainEnumQuantityUnit.UNIT),
            ai.shreds.shared.enums.SharedEnumAlertStatus.valueOf(jpaEntity.getStatus()),
            jpaEntity.getCreatedAt(),
            jpaEntity.getAcknowledgedAt(),
            jpaEntity.getResolvedAt()
        );
    }

    // ERP Reconciliation mappings
    @Mapping(target = "reconciliationId", expression = "java(reconciliation.getReconciliationId() != null ? reconciliation.getReconciliationId().getValue() : null)")
    @Mapping(target = "batchId", expression = "java(reconciliation.getBatchId() != null ? reconciliation.getBatchId().getValue() : null)")
    @Mapping(target = "status", expression = "java(reconciliation.getStatus() != null ? reconciliation.getStatus().name() : null)")
    @Mapping(target = "processedAt", source = "processedAt")
    @Mapping(target = "totalRecords", source = "totalRecords")
    @Mapping(target = "successCount", source = "successCount")
    @Mapping(target = "errorCount", source = "errorCount")
    @Mapping(target = "errors", source = "errors", qualifiedByName = "errorsToJson")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InfrastructureJpaEntityERPReconciliation toJpaEntity(DomainEntityERPReconciliation reconciliation);

    default DomainEntityERPReconciliation toDomainEntity(InfrastructureJpaEntityERPReconciliation jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityERPReconciliation.reconstruct(
            new DomainValueReconciliationId(jpaEntity.getReconciliationId()),
            new DomainValueBatchId(jpaEntity.getBatchId()),
            ai.shreds.domain.value_objects.DomainEnumReconciliationStatus.valueOf(jpaEntity.getStatus()),
            jpaEntity.getProcessedAt(),
            jpaEntity.getTotalRecords(),
            jpaEntity.getSuccessCount(),
            jpaEntity.getErrorCount(),
            jsonToErrors(jpaEntity.getErrors())
        );
    }

    // SKU mappings
    @Mapping(target = "skuId", expression = "java(sku.getSkuId() != null ? sku.getSkuId().getValue() : null)")
    @Mapping(target = "productId", expression = "java(sku.getProductId() != null ? sku.getProductId().getValue() : null)")
    @Mapping(target = "vendorSku", expression = "java(sku.getVendorSku() != null ? sku.getVendorSku().getValue() : null)")
    @Mapping(target = "active", expression = "java(sku.isActive())") // Revert back to 'active' for MapStruct bean property mapping
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    InfrastructureJpaEntitySKU toJpaEntity(DomainEntitySKU sku);

    default DomainEntitySKU toDomainEntity(InfrastructureJpaEntitySKU jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntitySKU.reconstruct(
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueProductId(jpaEntity.getProductId()),
            new DomainValueVendorSku(jpaEntity.getVendorSku()),
            jpaEntity.getIsActive(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getUpdatedAt()
        );
    }

    // Location mappings
    @Mapping(target = "locationId", expression = "java(location.getLocationId() != null ? location.getLocationId().getValue() : null)")
    @Mapping(target = "name", expression = "java(location.getName() != null ? location.getName().getValue() : null)")
    @Mapping(target = "type", expression = "java(location.getType() != null ? location.getType().name() : null)")
    @Mapping(target = "address", source = "address", qualifiedByName = "addressToJpaEmbeddable")
    @Mapping(target = "active", expression = "java(location.isActive())") // Revert back to 'active' for MapStruct bean property mapping
    @Mapping(target = "createdAt", source = "createdAt")
    InfrastructureJpaEntityLocation toJpaEntity(DomainEntityLocation location);

    default DomainEntityLocation toDomainEntity(InfrastructureJpaEntityLocation jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityLocation.reconstruct(
            new DomainValueLocationId(jpaEntity.getLocationId()),
            new DomainValueLocationName(jpaEntity.getName()),
            ai.shreds.shared.enums.SharedEnumLocationType.valueOf(jpaEntity.getType()),
            jpaEmbeddableToAddress(jpaEntity.getAddress()),
            jpaEntity.getIsActive(),
            jpaEntity.getCreatedAt()
        );
    }

    // Reservation mappings
    @Mapping(target = "reservationId", expression = "java(reservation.getReservationId() != null ? reservation.getReservationId().getValue() : null)")
    @Mapping(target = "skuId", expression = "java(reservation.getSkuId() != null ? reservation.getSkuId().getValue() : null)")
    @Mapping(target = "locationId", expression = "java(reservation.getLocationId() != null ? reservation.getLocationId().getValue() : null)")
    @Mapping(target = "quantity", expression = "java(reservation.getQuantity() != null ? reservation.getQuantity().getValue() : null)")
    @Mapping(target = "status", expression = "java(reservation.getStatus() != null ? reservation.getStatus().name() : null)")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "reason", source = "reason")
    InfrastructureJpaEntityReservation toJpaEntity(DomainEntityReservation reservation);

    default DomainEntityReservation toDomainEntity(InfrastructureJpaEntityReservation jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityReservation.reconstruct(
            new DomainValueReservationId(jpaEntity.getReservationId()),
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueLocationId(jpaEntity.getLocationId()),
            new DomainValueQuantity(jpaEntity.getQuantity(), DomainEnumQuantityUnit.UNIT),
            ai.shreds.domain.value_objects.DomainEnumReservationStatus.valueOf(jpaEntity.getStatus()),
            jpaEntity.getExpiresAt(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getReason()
        );
    }

    // Outbox Event mappings
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "aggregateId", source = "aggregateId")
    @Mapping(target = "aggregateType", source = "aggregateType")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "occurredOn", source = "occurredOn")
    @Mapping(target = "processed", source = "processed")
    @Mapping(target = "processedOn", source = "processedOn")
    InfrastructureJpaEntityOutboxEvent toJpaEntity(DomainEntityOutboxEvent event);

    default DomainEntityOutboxEvent toDomainEntity(InfrastructureJpaEntityOutboxEvent jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityOutboxEvent.reconstruct(
            jpaEntity.getEventId(),
            jpaEntity.getAggregateId(),
            jpaEntity.getAggregateType(),
            jpaEntity.getEventType(),
            jpaEntity.getPayload(),
            jpaEntity.getOccurredOn(),
            jpaEntity.getProcessed(),
            jpaEntity.getProcessedOn()
        );
    }

    // Stock Adjustment Audit mappings
    @Mapping(target = "auditId", source = "auditId")
    @Mapping(target = "ledgerId", expression = "java(audit.getLedgerId() != null ? audit.getLedgerId().getValue() : null)")
    @Mapping(target = "skuId", expression = "java(audit.getSkuId() != null ? audit.getSkuId().getValue() : null)")
    @Mapping(target = "locationId", expression = "java(audit.getLocationId() != null ? audit.getLocationId().getValue() : null)")
    @Mapping(target = "deltaQuantity", source = "deltaQuantity")
    @Mapping(target = "previousQuantity", source = "previousQuantity")
    @Mapping(target = "newQuantity", source = "newQuantity")
    @Mapping(target = "reason", expression = "java(audit.getReason() != null ? audit.getReason().name() : null)")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "userId", source = "userId")
    InfrastructureJpaEntityStockAdjustmentAudit toJpaEntity(DomainEntityStockAdjustmentAudit audit);

    default DomainEntityStockAdjustmentAudit toDomainEntity(InfrastructureJpaEntityStockAdjustmentAudit jpaEntity) {
        if (jpaEntity == null) return null;
        return DomainEntityStockAdjustmentAudit.reconstruct(
            jpaEntity.getAuditId(),
            new DomainValueLedgerId(jpaEntity.getLedgerId()),
            new DomainValueSkuId(jpaEntity.getSkuId()),
            new DomainValueLocationId(jpaEntity.getLocationId()),
            jpaEntity.getDeltaQuantity(),
            jpaEntity.getPreviousQuantity(),
            jpaEntity.getNewQuantity(),
            ai.shreds.shared.enums.SharedEnumAdjustmentReason.valueOf(jpaEntity.getReason()),
            jpaEntity.getSource(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getUserId()
        );
    }

    // List mappings
    List<DomainEntitySafetyStockRule> toDomainEntities(List<InfrastructureJpaEntitySafetyStockRule> jpaEntities);
    List<InfrastructureJpaEntitySafetyStockRule> toJpaEntities(List<DomainEntitySafetyStockRule> domainEntities);

    List<DomainEntityReservation> toDomainReservations(List<InfrastructureJpaEntityReservation> jpaEntities);

    // Helper methods for complex conversions
    @Named("addressToJpaEmbeddable")
    default InfrastructureJpaEmbeddableAddress addressToJpaEmbeddable(DomainValueAddress address) {
        if (address == null) return null;
        InfrastructureJpaEmbeddableAddress embeddable = new InfrastructureJpaEmbeddableAddress();
        embeddable.setStreet(address.getStreet());
        embeddable.setCity(address.getCity());
        embeddable.setState(address.getState());
        embeddable.setPostalCode(address.getPostalCode());
        embeddable.setCountry(address.getCountry());
        return embeddable;
    }

    @Named("jpaEmbeddableToAddress")
    default DomainValueAddress jpaEmbeddableToAddress(InfrastructureJpaEmbeddableAddress embeddable) {
        if (embeddable == null) return null;
        return new DomainValueAddress(
            embeddable.getStreet(),
            embeddable.getCity(),
            embeddable.getState(),
            embeddable.getPostalCode(),
            embeddable.getCountry()
        );
    }

    @Named("errorsToJson")
    default String errorsToJson(List<DomainValueReconciliationError> errors) {
        if (errors == null || errors.isEmpty()) return "[]";
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(errors);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing reconciliation errors", e);
        }
    }

    @Named("jsonToErrors")
    default List<DomainValueReconciliationError> jsonToErrors(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<DomainValueReconciliationError>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing reconciliation errors", e);
        }
    }
}