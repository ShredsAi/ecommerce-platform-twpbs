package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainAddressEntity;
import java.util.UUID;

public interface InfrastructureAddressJpaRepository extends JpaRepository<DomainAddressEntity, UUID> {
}
