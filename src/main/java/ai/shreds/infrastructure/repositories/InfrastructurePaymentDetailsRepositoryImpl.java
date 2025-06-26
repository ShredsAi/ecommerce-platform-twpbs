package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.ports.DomainOutputPortPaymentDetailsRepository;
import ai.shreds.infrastructure.mappers.InfrastructureOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class InfrastructurePaymentDetailsRepositoryImpl implements DomainOutputPortPaymentDetailsRepository {

    private final InfrastructureJPAPaymentDetailsRepository jpaRepository;
    private final InfrastructureOrderMapper mapper;

    public InfrastructurePaymentDetailsRepositoryImpl(InfrastructureJPAPaymentDetailsRepository jpaRepository,
                                                      InfrastructureOrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DomainPaymentDetailsEntity save(DomainPaymentDetailsEntity paymentDetails) {
        var jpaEntity = mapper.toJPAPaymentDetails(paymentDetails);
        var saved = jpaRepository.save(jpaEntity);
        return mapper.toDomainPaymentDetails(saved);
    }

    @Override
    public Optional<DomainPaymentDetailsEntity> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId)
                .map(mapper::toDomainPaymentDetails);
    }
}