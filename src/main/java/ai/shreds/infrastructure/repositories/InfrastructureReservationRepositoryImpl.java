package ai.shreds.infrastructure.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityReservation;
import ai.shreds.domain.value_objects.DomainEnumReservationStatus;
import ai.shreds.domain.value_objects.DomainValueReservationId;
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

    @Override
    public Optional<DomainEntityReservation> findById(DomainValueReservationId reservationId) {
        try {
            log.debug("Finding reservation by ID: {}", reservationId.getValue());
            
            Optional<InfrastructureJpaEntityReservation> jpaEntity = jpaRepository
                .findById(reservationId.getValue());
            
            if (jpaEntity.isPresent()) {
                DomainEntityReservation domainEntity = entityMapper.toDomainEntity(jpaEntity.get());
                log.debug("Found reservation: {}", reservationId.getValue());
                return Optional.of(domainEntity);
            } else {
                log.debug("Reservation not found: {}", reservationId.getValue());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to find reservation by ID: {}", reservationId.getValue(), e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to find reservation by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DomainEntityReservation> findExpiredReservations(int batchSize) {
        try {
            log.debug("Finding expired reservations with batch size: {}", batchSize);
            
            List<InfrastructureJpaEntityReservation> expiredReservations = jpaRepository
                .findExpiredReservations(Instant.now());
                
            List<DomainEntityReservation> domainReservations = expiredReservations.stream()
                .map(entityMapper::toDomainEntity)
                .collect(Collectors.toList());
                
            log.debug("Found {} expired reservations", domainReservations.size());
            return domainReservations;
        } catch (Exception e) {
            log.error("Failed to find expired reservations", e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to find expired reservations: " + e.getMessage(), e);
        }
    }
}
