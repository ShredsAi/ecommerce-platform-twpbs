package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityStockLedger;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureJpaStockLedgerRepository extends JpaRepository<InfrastructureJpaEntityStockLedger, UUID> {

    /**
     * Finds a stock ledger entry by SKU ID and location ID.
     */
    @Query("SELECT s FROM InfrastructureJpaEntityStockLedger s WHERE s.skuId = :skuId AND s.locationId = :locationId")
    Optional<InfrastructureJpaEntityStockLedger> findBySkuIdAndLocationId(
        @Param("skuId") String skuId,
        @Param("locationId") String locationId);

    /**
     * Locks and finds a stock ledger entry by its ID using pessimistic locking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InfrastructureJpaEntityStockLedger s WHERE s.ledgerId = :ledgerId")
    Optional<InfrastructureJpaEntityStockLedger> findByIdWithPessimisticLock(@Param("ledgerId") UUID ledgerId);
}
