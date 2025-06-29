package ai.shreds.infrastructure.repositories;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityStockAdjustmentAudit;
import ai.shreds.domain.ports.DomainOutputPortStockAdjustmentAuditRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityStockAdjustmentAudit;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureStockAdjustmentAuditRepositoryImpl implements DomainOutputPortStockAdjustmentAuditRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureStockAdjustmentAuditRepositoryImpl.class);

    private final InfrastructureJpaStockAdjustmentAuditRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureStockAdjustmentAuditRepositoryImpl(InfrastructureJpaStockAdjustmentAuditRepository jpaRepository,
                                                            InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public void save(DomainEntityStockAdjustmentAudit audit) {
        try {
            log.debug("Saving stock adjustment audit for ledger: {} (SKU: {}, Location: {})", 
                audit.getLedgerId().getValue(), 
                audit.getSkuId().getValue(), 
                audit.getLocationId().getValue());
                
            InfrastructureJpaEntityStockAdjustmentAudit jpa = entityMapper.toJpaEntity(audit);
            jpaRepository.save(jpa);
            
            log.debug("Successfully saved stock adjustment audit: {}", jpa.getAuditId());
        } catch (Exception e) {
            log.error("Failed to save stock adjustment audit", e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to save stock adjustment audit: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void saveAll(List<DomainEntityStockAdjustmentAudit> audits) {
        if (audits == null || audits.isEmpty()) {
            log.debug("No audit records to save");
            return;
        }
        
        try {
            log.debug("Saving {} stock adjustment audit records in batch", audits.size());
            
            List<InfrastructureJpaEntityStockAdjustmentAudit> entities = audits.stream()
                .map(entityMapper::toJpaEntity)
                .collect(Collectors.toList());
                
            List<InfrastructureJpaEntityStockAdjustmentAudit> saved = jpaRepository.saveAll(entities);
            
            log.debug("Successfully saved {} stock adjustment audit records", saved.size());
        } catch (Exception e) {
            log.error("Failed to save batch of {} stock adjustment audit records", audits.size(), e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to save batch stock adjustment audits: " + e.getMessage(), e);
        }
    }
}
