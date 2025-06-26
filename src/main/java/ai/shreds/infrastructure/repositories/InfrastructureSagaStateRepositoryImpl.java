package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.domain.ports.DomainOutputPortSagaStateRepository;
import ai.shreds.infrastructure.mappers.InfrastructureOrderMapper;
import ai.shreds.shared.enums.SharedSagaStatusEnum;
import ai.shreds.infrastructure.external_services.InfrastructureRedisCache;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class InfrastructureSagaStateRepositoryImpl implements DomainOutputPortSagaStateRepository {

    private final InfrastructureJPASagaStateRepository jpaRepository;
    private final InfrastructureOrderMapper mapper;
    private final InfrastructureRedisCache redisCache;

    public InfrastructureSagaStateRepositoryImpl(InfrastructureJPASagaStateRepository jpaRepository,
                                                 InfrastructureOrderMapper mapper,
                                                 InfrastructureRedisCache redisCache) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.redisCache = redisCache;
    }

    @Override
    public DomainSagaStateEntity save(DomainSagaStateEntity sagaState) {
        var jpaEntity = mapper.toJPASagaState(sagaState);
        var saved = jpaRepository.save(jpaEntity);
        // cache the saved state
        String key = "saga:" + saved.getOrderId();
        redisCache.put(key, mapper.toDomainSagaState(saved), Duration.ofMillis(sagaState.getTimeoutThreshold()));
        return mapper.toDomainSagaState(saved);
    }

    @Override
    public Optional<DomainSagaStateEntity> findByOrderId(UUID orderId) {
        String key = "saga:" + orderId;
        Optional<DomainSagaStateEntity> cached = redisCache.get(key, DomainSagaStateEntity.class);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<InfrastructureSagaStateJPAEntity> jpaOpt = jpaRepository.findByOrderId(orderId);
        Optional<DomainSagaStateEntity> domainOpt = jpaOpt.map(mapper::toDomainSagaState);
        domainOpt.ifPresent(entity -> redisCache.put(key, entity, Duration.ofMillis(entity.getTimeoutThreshold())));
        return domainOpt;
    }

    @Override
    public List<DomainSagaStateEntity> findTimedOut(LocalDateTime cutoff) {
        Instant cutoffInstant = cutoff.toInstant(ZoneOffset.UTC);
        List<InfrastructureSagaStateJPAEntity> jpas = jpaRepository.findByStatusAndLastActivityBefore(
                SharedSagaStatusEnum.IN_PROGRESS.name(), cutoffInstant);
        return jpas.stream()
                .map(mapper::toDomainSagaState)
                .collect(Collectors.toList());
    }
}