package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import ai.shreds.domain.ports.DomainOutputPortRefundTransactionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of DomainOutputPortRefundTransactionRepository using JPA.
 */
@Repository
public class InfrastructureRefundTransactionRepositoryImpl implements DomainOutputPortRefundTransactionRepository {

    private final InfrastructureJpaRefundTransactionRepository jpaRepository;

    public InfrastructureRefundTransactionRepositoryImpl(InfrastructureJpaRefundTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DomainRefundTransactionEntity save(DomainRefundTransactionEntity refund) {
        return jpaRepository.save(refund);
    }

    @Override
    public DomainRefundTransactionEntity findById(String refundId) {
        return jpaRepository.findById(refundId).orElse(null);
    }

    @Override
    public List<DomainRefundTransactionEntity> findByCancellationId(String cancellationId) {
        return jpaRepository.findByCancellationId(cancellationId);
    }

    @Override
    public List<DomainRefundTransactionEntity> findByReturnId(String returnId) {
        return jpaRepository.findByReturnId(returnId);
    }
}