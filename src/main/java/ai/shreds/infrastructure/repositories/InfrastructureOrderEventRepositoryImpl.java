package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.ports.DomainOutputPortOrderEventRepository;
import ai.shreds.infrastructure.exceptions.InfrastructurePersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class InfrastructureOrderEventRepositoryImpl implements DomainOutputPortOrderEventRepository {

    private final InfrastructureJpaOrderEventRepository jpaOrderEventRepository;

    @Autowired
    public InfrastructureOrderEventRepositoryImpl(InfrastructureJpaOrderEventRepository jpaOrderEventRepository) {
        this.jpaOrderEventRepository = jpaOrderEventRepository;
    }

    @Override
    @Transactional
    public DomainOrderEventEntity save(DomainOrderEventEntity event) {
        try {
            return jpaOrderEventRepository.save(event);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainOrderEventEntity", "save", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainOrderEventEntity> findByOrderId(String orderId) {
        try {
            return jpaOrderEventRepository.findByOrderIdOrderByTimestampDesc(orderId);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainOrderEventEntity", "findByOrderId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainOrderEventEntity> findByEventType(String eventType) {
        try {
            return jpaOrderEventRepository.findByEventType(eventType);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainOrderEventEntity", "findByEventType", e);
        }
    }
}