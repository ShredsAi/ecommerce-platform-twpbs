package ai.shreds.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityLowStockAlert;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.domain.ports.DomainOutputPortLowStockAlertRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityLowStockAlert;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;
import ai.shreds.shared.enums.SharedEnumAlertStatus;

@Repository
public class InfrastructureLowStockAlertRepositoryImpl implements DomainOutputPortLowStockAlertRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureLowStockAlertRepositoryImpl.class);

    private final InfrastructureJpaLowStockAlertRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureLowStockAlertRepositoryImpl(InfrastructureJpaLowStockAlertRepository jpaRepository,
                                                     InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<DomainEntityLowStockAlert> findUnresolvedBySkuIdAndLocationId(String skuId, String locationId) {
        try {
            log.debug("Finding unresolved low stock alert for SKU: {} at location: {}", skuId, locationId);
            
            Optional<InfrastructureJpaEntityLowStockAlert> alertOpt = jpaRepository
                .findBySkuIdAndLocationIdAndStatus(skuId, locationId, SharedEnumAlertStatus.PENDING.name());
            
            if (alertOpt.isPresent()) {
                log.debug("Unresolved alert found for SKU: {} at location: {}", skuId, locationId);
                return alertOpt.map(entityMapper::toDomainEntity);
            } else {
                log.debug("No unresolved alert found for SKU: {} at location: {}", skuId, locationId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to find unresolved alert for SKU: {} at location: {}", skuId, locationId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to find unresolved low stock alert: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public DomainEntityLowStockAlert save(DomainEntityLowStockAlert alert) {
        try {
            log.debug("Saving low stock alert for SKU: {} at location: {}", 
                alert.getSkuId().getValue(), alert.getLocationId().getValue());
                
            InfrastructureJpaEntityLowStockAlert jpa = entityMapper.toJpaEntity(alert);
            InfrastructureJpaEntityLowStockAlert saved = jpaRepository.save(jpa);
            
            log.debug("Successfully saved low stock alert: {}", saved.getAlertId());
            return entityMapper.toDomainEntity(saved);
        } catch (Exception e) {
            log.error("Failed to save low stock alert", e);
            throw new InfrastructureExceptionDatabaseError("Failed to save low stock alert: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateStatus(UUID alertId, String status) {
        try {
            log.debug("Updating alert status to {} for alert: {}", status, alertId);
            
            InfrastructureJpaEntityLowStockAlert alert = jpaRepository.findById(alertId)
                .orElseThrow(() -> new DomainExceptionEntityNotFound("LowStockAlert", alertId.toString()));
                
            alert.setStatus(status);
            
            if ("ACKNOWLEDGED".equals(status)) {
                alert.setAcknowledgedAt(Instant.now());
            } else if ("RESOLVED".equals(status)) {
                alert.setResolvedAt(Instant.now());
                if (alert.getAcknowledgedAt() == null) {
                    alert.setAcknowledgedAt(Instant.now());
                }
            }
            
            jpaRepository.save(alert);
            
            log.debug("Successfully updated alert status for alert: {}", alertId);
        } catch (DomainExceptionEntityNotFound e) {
            log.warn("Alert not found for status update: {}", alertId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to update alert status for alert: {}", alertId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to update alert status: " + e.getMessage(), e);
        }
    }
}
