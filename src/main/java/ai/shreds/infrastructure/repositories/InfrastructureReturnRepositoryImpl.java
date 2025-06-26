package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.domain.ports.DomainOutputPortReturnRepository;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of DomainOutputPortReturnRepository using JPA.
 */
@Repository
public class InfrastructureReturnRepositoryImpl implements DomainOutputPortReturnRepository {

    private final InfrastructureJpaReturnRepository jpaRepository;

    public InfrastructureReturnRepositoryImpl(InfrastructureJpaReturnRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DomainReturnRequestEntity save(DomainReturnRequestEntity returnRequest) {
        return jpaRepository.save(returnRequest);
    }

    @Override
    public DomainReturnRequestEntity findById(String returnId) {
        return jpaRepository.findById(returnId).orElse(null);
    }

    @Override
    public List<DomainReturnRequestEntity> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<DomainReturnRequestEntity> findByReturnStatus(SharedReturnStatusEnum status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public DomainReturnRequestEntity findByRmaNumber(String rmaNumber) {
        return jpaRepository.findByRmaNumber(rmaNumber).orElse(null);
    }
}