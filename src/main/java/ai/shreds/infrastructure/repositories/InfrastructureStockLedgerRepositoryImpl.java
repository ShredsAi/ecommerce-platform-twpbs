package ai.shreds.infrastructure.repositories;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityStockLedger;
import ai.shreds.domain.ports.DomainOutputPortStockLedgerRepository;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityStockLedger;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;

@Repository
public class InfrastructureStockLedgerRepositoryImpl implements DomainOutputPortStockLedgerRepository {

    private final InfrastructureJpaStockLedgerRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureStockLedgerRepositoryImpl(InfrastructureJpaStockLedgerRepository jpaRepository,
                                                   InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<DomainEntityStockLedger> findBySkuIdAndLocationId(String skuId, String locationId) {
        return jpaRepository.findBySkuIdAndLocationId(skuId, locationId)
                             .map(entityMapper::toDomainEntity);
    }

    @Override
    @Transactional
    public DomainEntityStockLedger save(DomainEntityStockLedger ledger) {
        InfrastructureJpaEntityStockLedger jpa = entityMapper.toJpaEntity(ledger);
        InfrastructureJpaEntityStockLedger saved = jpaRepository.save(jpa);
        return entityMapper.toDomainEntity(saved);
    }

    @Override
    @Transactional
    public void updateQuantityAtomic(UUID ledgerId, BigDecimal deltaQuantity) {
        InfrastructureJpaEntityStockLedger entity = jpaRepository.findByIdWithPessimisticLock(ledgerId)
            .orElseThrow(() -> new DomainExceptionEntityNotFound("StockLedger", ledgerId.toString()));
        entity.setQuantity(entity.getQuantity().add(deltaQuantity));
        entity.setAvailable(entity.getQuantity().subtract(entity.getReserved()));
        entity.setLastUpdated(Instant.now());
        jpaRepository.save(entity);
    }

    @Override
    public DomainEntityStockLedger lockById(UUID ledgerId) {
        return jpaRepository.findByIdWithPessimisticLock(ledgerId)
                .map(entityMapper::toDomainEntity)
                .orElseThrow(() -> new DomainExceptionEntityNotFound("StockLedger", ledgerId.toString()));
    }
}
