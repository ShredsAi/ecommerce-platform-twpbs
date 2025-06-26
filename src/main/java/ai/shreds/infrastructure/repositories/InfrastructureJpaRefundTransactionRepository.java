package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfrastructureJpaRefundTransactionRepository extends JpaRepository<DomainRefundTransactionEntity, String> {

    Optional<DomainRefundTransactionEntity> findById(String id);

    @Query("SELECT r FROM DomainRefundTransactionEntity r WHERE r.cancellationId = :cancellationId")
    List<DomainRefundTransactionEntity> findByCancellationId(@Param("cancellationId") String cancellationId);

    @Query("SELECT r FROM DomainRefundTransactionEntity r WHERE r.returnId = :returnId")
    List<DomainRefundTransactionEntity> findByReturnId(@Param("returnId") String returnId);

    @Query("SELECT r FROM DomainRefundTransactionEntity r WHERE r.status = :status")
    List<DomainRefundTransactionEntity> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM DomainRefundTransactionEntity r WHERE r.orderId = :orderId")
    List<DomainRefundTransactionEntity> findByOrderId(@Param("orderId") String orderId);
}