package ai.shreds.infrastructure.repositories;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityReservation;
import ai.shreds.domain.value_objects.DomainEnumReservationStatus;
import ai.shreds.domain.ports.DomainOutputPortReservationRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityReservation;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureReservationRepositoryImpl implements DomainOutputPortReservationRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureReservationRepositoryImpl.class);

    private final InfrastructureJpaReservationRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureReservationRepositoryImpl(InfrastructureJpaReservationRepository jpaRepository,
            InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public DomainEntityReservation save(DomainEntityReservation reservation) {
        try {
            log.debug("Saving reservation for SKU: {} at location: {}", 
                reservation.getSkuId().getValue(), reservation.getLocationId().getValue());
                
            InfrastructureJpaEntityReservation jpaEntity = entityMapper.toJpaEntity(reservation);
            InfrastructureJpaEntityReservation saved = jpaRepository.save(jpaEntity);
            
            log.debug("Successfully saved reservation: {}", saved.getReservationId());
            return entityMapper.toDomainEntity(saved);
        } catch (Exception e) {
            log.error("Failed to save reservation", e);
            throw new InfrastructureExceptionDatabaseError("Failed to save reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DomainEntityReservation> findActiveBySkuAndLocation(String skuId, String locationId) {
        try {
            log.debug("Finding active reservations for SKU: {} at location: {}", skuId, locationId);
            
            List<String> activeStatuses = List.of(
                DomainEnumReservationStatus.PENDING.name(),
                DomainEnumReservationStatus.CONFIRMED.name()
            );
            
            List<InfrastructureJpaEntityReservation> jpaEntities = jpaRepository
                .findBySkuIdAndLocationIdAndStatusIn(skuId, locationId, activeStatuses);
                
            List<DomainEntityReservation> reservations = jpaEntities.stream()
                .map(entityMapper::toDomainEntity)
                .collect(Collectors.toList());
                
            log.debug("Found {} active reservations for SKU: {} at location: {}", 
                reservations.size(), skuId, locationId);
                
            return reservations;
        } catch (Exception e) {
            log.error("Failed to find active reservations for SKU: {} at location: {}", skuId, locationId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to find active reservations: " + e.getMessage(), e);
        }
    }
}
