package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.application.ports.DomainOutputPortCancellationRepository;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.infrastructure.exceptions.InfrastructurePersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class InfrastructureCancellationRepositoryImpl implements DomainOutputPortCancellationRepository {

    private final InfrastructureJpaCancellationRepository jpaRepository;

    @Autowired
    public InfrastructureCancellationRepositoryImpl(InfrastructureJpaCancellationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DomainCancellationRequestEntity save(DomainCancellationRequestEntity cancellation) {
        try {
            return jpaRepository.save(cancellation);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainCancellationRequestEntity", "save", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DomainCancellationRequestEntity findById(String cancellationId) {
        try {
            Optional<DomainCancellationRequestEntity> result = jpaRepository.findById(cancellationId);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainCancellationRequestEntity", "findById", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainCancellationRequestEntity> findByOrderId(String orderId) {
        try {
            return jpaRepository.findByOrderId(orderId);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainCancellationRequestEntity", "findByOrderId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainCancellationRequestEntity> findByCancellationStatus(SharedCancellationStatusEnum status) {
        try {
            return jpaRepository.findByStatus(status);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainCancellationRequestEntity", "findByCancellationStatus", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainCancellationRequestEntity> findPendingBefore(LocalDateTime cutoff) {
        try {
            return jpaRepository.findPendingBefore(cutoff);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainCancellationRequestEntity", "findPendingBefore", e);
        }
    }
}