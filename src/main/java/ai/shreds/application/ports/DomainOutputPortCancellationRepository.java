package ai.shreds.application.ports;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import java.time.LocalDateTime;
import java.util.List;

public interface DomainOutputPortCancellationRepository {

    DomainCancellationRequestEntity save(DomainCancellationRequestEntity cancellation);

    DomainCancellationRequestEntity findById(String cancellationId);

    List<DomainCancellationRequestEntity> findByOrderId(String orderId);

    List<DomainCancellationRequestEntity> findByCancellationStatus(SharedCancellationStatusEnum status);

    List<DomainCancellationRequestEntity> findPendingBefore(LocalDateTime cutoff);

}