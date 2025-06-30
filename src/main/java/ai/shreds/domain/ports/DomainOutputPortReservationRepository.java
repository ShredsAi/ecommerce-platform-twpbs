package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityReservation;
import ai.shreds.domain.value_objects.DomainValueReservationId;

import java.util.List;
import java.util.Optional;

public interface DomainOutputPortReservationRepository {
    DomainEntityReservation save(DomainEntityReservation reservation);
    List<DomainEntityReservation> findActiveBySkuAndLocation(String skuId, String locationId);
    Optional<DomainEntityReservation> findById(DomainValueReservationId reservationId);
    List<DomainEntityReservation> findExpiredReservations(int batchSize);
}