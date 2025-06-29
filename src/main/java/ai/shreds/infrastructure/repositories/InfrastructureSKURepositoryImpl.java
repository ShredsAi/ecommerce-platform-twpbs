package ai.shreds.infrastructure.repositories;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ai.shreds.domain.entities.DomainEntitySKU;
import ai.shreds.domain.ports.DomainOutputPortSKURepository;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntitySKU;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureSKURepositoryImpl implements DomainOutputPortSKURepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureSKURepositoryImpl.class);

    private final InfrastructureJpaSKURepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureSKURepositoryImpl(InfrastructureJpaSKURepository jpaRepository,
                                            InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<DomainEntitySKU> findById(String skuId) {
        try {
            log.debug("Finding SKU by ID: {}", skuId);
            
            Optional<InfrastructureJpaEntitySKU> jpaEntity = jpaRepository.findById(skuId);
            
            if (jpaEntity.isPresent()) {
                log.debug("SKU found: {}", skuId);
                return jpaEntity.map(entityMapper::toDomainEntity);
            } else {
                log.debug("SKU not found: {}", skuId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to find SKU by ID: {}", skuId, e);
            throw new InfrastructureExceptionDatabaseError("Failed to find SKU by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsAndActive(String skuId) {
        try {
            log.debug("Checking if SKU exists and is active: {}", skuId);
            
            boolean exists = jpaRepository.existsBySkuIdAndIsActive(skuId, true);
            
            log.debug("SKU {} exists and active: {}", skuId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check if SKU exists and is active: {}", skuId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to check SKU existence and status: " + e.getMessage(), e);
        }
    }
}
