package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityReservation;
import java.util.List;

public interface DomainOutputPortReservationRepository {
    DomainEntityReservation save(DomainEntityReservation reservation);
    List<DomainEntityReservation> findActiveBySkuAndLocation(String skuId, String locationId);
}
