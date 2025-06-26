package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.domain.ports.DomainOutputPortShippingDetailsRepository;
import ai.shreds.infrastructure.mappers.InfrastructureOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class InfrastructureShippingDetailsRepositoryImpl implements DomainOutputPortShippingDetailsRepository {

    private final InfrastructureJPAShippingDetailsRepository jpaRepository;
    private final InfrastructureOrderMapper mapper;

    public InfrastructureShippingDetailsRepositoryImpl(InfrastructureJPAShippingDetailsRepository jpaRepository,
                                                       InfrastructureOrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DomainShippingDetailsEntity save(DomainShippingDetailsEntity shippingDetails) {
        var jpaEntity = mapper.toJPAShippingDetails(shippingDetails);
        var saved = jpaRepository.save(jpaEntity);
        return mapper.toDomainShippingDetails(saved);
    }

    @Override
    public Optional<DomainShippingDetailsEntity> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId)
                .map(mapper::toDomainShippingDetails);
    }
}