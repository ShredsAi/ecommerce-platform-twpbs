package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.application.ports.DomainOutputPortReturnRepository;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.infrastructure.exceptions.InfrastructurePersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class InfrastructureReturnRepositoryImpl implements DomainOutputPortReturnRepository {

    private final InfrastructureJpaReturnRepository jpaReturnRepository;

    @Autowired
    public InfrastructureReturnRepositoryImpl(InfrastructureJpaReturnRepository jpaReturnRepository) {
        this.jpaReturnRepository = jpaReturnRepository;
    }

    @Override
    @Transactional
    public DomainReturnRequestEntity save(DomainReturnRequestEntity returnRequest) {
        try {
            return jpaReturnRepository.save(returnRequest);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainReturnRequestEntity", "save", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DomainReturnRequestEntity findById(String returnId) {
        try {
            Optional<DomainReturnRequestEntity> result = jpaReturnRepository.findById(returnId);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainReturnRequestEntity", "findById", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainReturnRequestEntity> findByOrderId(String orderId) {
        try {
            return jpaReturnRepository.findByOrderId(orderId);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainReturnRequestEntity", "findByOrderId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainReturnRequestEntity> findByReturnStatus(SharedReturnStatusEnum status) {
        try {
            return jpaReturnRepository.findByStatus(status);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainReturnRequestEntity", "findByReturnStatus", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DomainReturnRequestEntity findByRmaNumber(String rmaNumber) {
        try {
            Optional<DomainReturnRequestEntity> result = jpaReturnRepository.findByRmaNumber(rmaNumber);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainReturnRequestEntity", "findByRmaNumber", e);
        }
    }
}