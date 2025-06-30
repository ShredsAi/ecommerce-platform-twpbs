package ai.shreds.infrastructure.repositories;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityERPReconciliation;
import ai.shreds.domain.ports.DomainOutputPortERPReconciliationRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityERPReconciliation;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureERPReconciliationRepositoryImpl implements DomainOutputPortERPReconciliationRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureERPReconciliationRepositoryImpl.class);

    private final InfrastructureJpaERPReconciliationRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureERPReconciliationRepositoryImpl(InfrastructureJpaERPReconciliationRepository jpaRepository, InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public DomainEntityERPReconciliation save(DomainEntityERPReconciliation reconciliation) {
        try {
            log.debug("Saving ERP reconciliation with batch ID: {}", reconciliation.getBatchId().getValue());
            
            InfrastructureJpaEntityERPReconciliation jpaEntity = entityMapper.toJpaEntity(reconciliation);
            InfrastructureJpaEntityERPReconciliation saved = jpaRepository.save(jpaEntity);
            
            log.debug("Successfully saved ERP reconciliation: {}", saved.getReconciliationId());
            return entityMapper.toDomainEntity(saved);
        } catch (Exception e) {
            log.error("Failed to save ERP reconciliation", e);
            throw new InfrastructureExceptionDatabaseError("Failed to save ERP reconciliation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DomainEntityERPReconciliation> findByBatchId(String batchId) {
        try {
            log.debug("Finding ERP reconciliation by batch ID: {}", batchId);
            
            Optional<InfrastructureJpaEntityERPReconciliation> entityOpt = jpaRepository.findByBatchId(batchId);
            
            if (entityOpt.isPresent()) {
                log.debug("ERP reconciliation found for batch ID: {}", batchId);
            } else {
                log.warn("No ERP reconciliation found for batch ID: {}", batchId);
            }
            return entityOpt.map(entityMapper::toDomainEntity);
        } catch (Exception e) {
            log.error("Failed to find ERP reconciliation by batch ID: {}", batchId, e);
            throw new InfrastructureExceptionDatabaseError("Failed to find ERP reconciliation by batch ID: " + e.getMessage(), e);
        }
    }

    // Additional methods could be added here for retrieving lists or other operations
}
