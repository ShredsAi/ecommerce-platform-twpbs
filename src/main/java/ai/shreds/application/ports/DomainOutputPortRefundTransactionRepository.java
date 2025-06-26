package ai.shreds.application.ports;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import java.util.List;

public interface DomainOutputPortRefundTransactionRepository {

    DomainRefundTransactionEntity save(DomainRefundTransactionEntity refund);

    DomainRefundTransactionEntity findById(String refundId);

    List<DomainRefundTransactionEntity> findByCancellationId(String cancellationId);

    List<DomainRefundTransactionEntity> findByReturnId(String returnId);

}
