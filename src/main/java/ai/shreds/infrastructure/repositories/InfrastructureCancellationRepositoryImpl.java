package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.domain.ports.DomainOutputPortCancellationRepository;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of DomainOutputPortCancellationRepository using JPA.
 */
@Repository
public class InfrastructureCancellationRepositoryImpl implements DomainOutputPortCancellationRepository {

    private final InfrastructureJpaCancellationRepository jpaRepository;

    public InfrastructureCancellationRepositoryImpl(InfrastructureJpaCancellationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DomainCancellationRequestEntity save(DomainCancellationRequestEntity cancellation) {
        return jpaRepository.save(cancellation);
    }

    @Override
    public DomainCancellationRequestEntity findById(String cancellationId) {
        return jpaRepository.findById(cancellationId).orElse(null);
    }

    @Override
    public List<DomainCancellationRequestEntity> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<DomainCancellationRequestEntity> findByCancellationStatus(SharedCancellationStatusEnum status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<DomainCancellationRequestEntity> findPendingBefore(LocalDateTime cutoff) {
        return jpaRepository.findByStatusAndRequestedAtBefore(SharedCancellationStatusEnum.PENDING, cutoff);
    }
}