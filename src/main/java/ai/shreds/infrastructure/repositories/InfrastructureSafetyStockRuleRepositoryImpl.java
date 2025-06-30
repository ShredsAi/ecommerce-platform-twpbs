package ai.shreds.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntitySafetyStockRule;
import ai.shreds.domain.ports.DomainOutputPortSafetyStockRuleRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntitySafetyStockRule;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;

@Repository
public class InfrastructureSafetyStockRuleRepositoryImpl implements DomainOutputPortSafetyStockRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureSafetyStockRuleRepositoryImpl.class);

    private final InfrastructureJpaSafetyStockRuleRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureSafetyStockRuleRepositoryImpl(InfrastructureJpaSafetyStockRuleRepository jpaRepository, InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<DomainEntitySafetyStockRule> findActiveBySkuIdAndLocationId(String skuId, String locationId) {
        log.debug("Finding active safety stock rule for SKU: {} at location: {}", skuId, locationId);
        return jpaRepository.findBySkuIdAndLocationIdAndIsActive(skuId, locationId, true)
                .map(entityMapper::toDomainEntity);
    }

    @Override
    @Transactional
    public DomainEntitySafetyStockRule save(DomainEntitySafetyStockRule rule) {
        log.debug("Saving safety stock rule for SKU: {} at location: {}", 
            rule.getSkuId().getValue(), rule.getLocationId().getValue());
        InfrastructureJpaEntitySafetyStockRule jpaEntity = entityMapper.toJpaEntity(rule);
        InfrastructureJpaEntitySafetyStockRule saved = jpaRepository.save(jpaEntity);
        return entityMapper.toDomainEntity(saved);
    }

    @Override
    public List<DomainEntitySafetyStockRule> findAll() {
        log.debug("Finding all active safety stock rules");
        return jpaRepository.findAllByIsActive(true).stream()
                .map(entityMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateAllBySkuId(String skuId) {
        log.debug("Deactivating all safety stock rules for SKU: {}", skuId);
        jpaRepository.deactivateAllBySkuId(skuId);
    }
}
