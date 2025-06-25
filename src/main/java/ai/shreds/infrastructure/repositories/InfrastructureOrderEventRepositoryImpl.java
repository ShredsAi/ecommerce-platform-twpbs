package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.ports.DomainOutputPortOrderEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of DomainOutputPortOrderEventRepository using JPA.
 */
@Repository
public class InfrastructureOrderEventRepositoryImpl implements DomainOutputPortOrderEventRepository {

    private final InfrastructureJpaOrderEventRepository jpaRepository;

    public InfrastructureOrderEventRepositoryImpl(InfrastructureJpaOrderEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DomainOrderEventEntity save(DomainOrderEventEntity event) {
        return jpaRepository.save(event);
    }

    @Override
    public List<DomainOrderEventEntity> findByOrderId(String orderId) {
        return jpaRepository.findByOrderIdOrderByTimestampDesc(orderId);
    }

    @Override
    public List<DomainOrderEventEntity> findByEventType(String eventType) {
        return jpaRepository.findByEventType(eventType);
    }
}