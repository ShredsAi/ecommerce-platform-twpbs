package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfrastructureJpaReturnRepository extends JpaRepository<DomainReturnRequestEntity, String> {

    Optional<DomainReturnRequestEntity> findById(String id);

    @Query("SELECT r FROM DomainReturnRequestEntity r WHERE r.orderId.value = :orderId")
    List<DomainReturnRequestEntity> findByOrderId(@Param("orderId") String orderId);

    @Query("SELECT r FROM DomainReturnRequestEntity r WHERE r.rmaNumber = :rmaNumber")
    Optional<DomainReturnRequestEntity> findByRmaNumber(@Param("rmaNumber") String rmaNumber);

    @Query("SELECT r FROM DomainReturnRequestEntity r WHERE r.status = :status")
    List<DomainReturnRequestEntity> findByStatus(@Param("status") SharedReturnStatusEnum status);

    @Query("SELECT r FROM DomainReturnRequestEntity r WHERE r.customerId.value = :customerId")
    List<DomainReturnRequestEntity> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT r FROM DomainReturnRequestEntity r WHERE r.status IN :statuses")
    List<DomainReturnRequestEntity> findByStatusIn(@Param("statuses") List<SharedReturnStatusEnum> statuses);
}