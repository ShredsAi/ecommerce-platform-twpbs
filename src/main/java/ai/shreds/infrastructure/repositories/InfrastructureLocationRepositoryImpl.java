package ai.shreds.infrastructure.repositories;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ai.shreds.domain.entities.DomainEntityLocation;
import ai.shreds.domain.ports.DomainOutputPortLocationRepository;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityLocation;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureLocationRepositoryImpl implements DomainOutputPortLocationRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureLocationRepositoryImpl.class);

    private final InfrastructureJpaLocationRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureLocationRepositoryImpl(InfrastructureJpaLocationRepository jpaRepository,
                                                InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<DomainEntityLocation> findById(String locationId) {
        try {
            log.debug("Finding location by ID: {}", locationId);
            
            Optional<InfrastructureJpaEntityLocation> jpaEntity = jpaRepository.findById(locationId);
            
            if (jpaEntity.isPresent()) {
                log.debug("Location found: {}", locationId);
                return jpaEntity.map(entityMapper::toDomainEntity);
            } else {
                log.debug("Location not found: {}", locationId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to find location by ID: {}", locationId, e);
            throw new InfrastructureExceptionDatabaseError("Failed to find location by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsAndActive(String locationId) {
        try {
            log.debug("Checking if location exists and is active: {}", locationId);
            
            boolean exists = jpaRepository.existsByLocationIdAndIsActive(locationId, true);
            
            log.debug("Location {} exists and active: {}", locationId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check if location exists and is active: {}", locationId, e);
            throw new InfrastructureExceptionDatabaseError(
                "Failed to check location existence and status: " + e.getMessage(), e);
        }
    }
}
