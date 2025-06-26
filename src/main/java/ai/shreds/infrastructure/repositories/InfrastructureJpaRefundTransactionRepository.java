package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for refund transaction entities.
 */
@Repository
public interface InfrastructureJpaRefundTransactionRepository extends JpaRepository<DomainRefundTransactionEntity, String> {

    Optional<DomainRefundTransactionEntity> findById(String id);

    List<DomainRefundTransactionEntity> findByCancellationId(String cancellationId);

    List<DomainRefundTransactionEntity> findByReturnId(String returnId);
}