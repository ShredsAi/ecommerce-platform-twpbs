package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityLocation;
import java.util.Optional;

public interface DomainOutputPortLocationRepository {
    Optional<DomainEntityLocation> findById(String locationId);
    boolean existsAndActive(String locationId);
}
