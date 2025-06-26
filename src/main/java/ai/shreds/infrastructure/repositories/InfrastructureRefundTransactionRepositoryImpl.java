package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import ai.shreds.application.ports.DomainOutputPortRefundTransactionRepository;
import ai.shreds.infrastructure.exceptions.InfrastructurePersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class InfrastructureRefundTransactionRepositoryImpl implements DomainOutputPortRefundTransactionRepository {

    private final InfrastructureJpaRefundTransactionRepository jpaRefundRepository;

    @Autowired
    public InfrastructureRefundTransactionRepositoryImpl(InfrastructureJpaRefundTransactionRepository jpaRefundRepository) {
        this.jpaRefundRepository = jpaRefundRepository;
    }

    @Override
    @Transactional
    public DomainRefundTransactionEntity save(DomainRefundTransactionEntity refund) {
        try {
            return jpaRefundRepository.save(refund);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainRefundTransactionEntity", "save", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DomainRefundTransactionEntity findById(String refundId) {
        try {
            Optional<DomainRefundTransactionEntity> result = jpaRefundRepository.findById(refundId);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainRefundTransactionEntity", "findById", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainRefundTransactionEntity> findByCancellationId(String cancellationId) {
        try {
            return jpaRefundRepository.findByCancellationId(cancellationId);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainRefundTransactionEntity", "findByCancellationId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainRefundTransactionEntity> findByReturnId(String returnId) {
        try {
            return jpaRefundRepository.findByReturnId(returnId);
        } catch (DataAccessException e) {
            throw new InfrastructurePersistenceException("DomainRefundTransactionEntity", "findByReturnId", e);
        }
    }

}