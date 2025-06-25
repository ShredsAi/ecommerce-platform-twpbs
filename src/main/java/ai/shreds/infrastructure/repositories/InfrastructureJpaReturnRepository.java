package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for return request entities.
 */
@Repository
public interface InfrastructureJpaReturnRepository extends JpaRepository<DomainReturnRequestEntity, String> {

    Optional<DomainReturnRequestEntity> findById(String id);

    List<DomainReturnRequestEntity> findByOrderId(String orderId);

    List<DomainReturnRequestEntity> findByStatus(SharedReturnStatusEnum status);

    Optional<DomainReturnRequestEntity> findByRmaNumber(String rmaNumber);
}