package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentStatusUpdate;
import ai.shreds.domain.ports.DomainOutputPortStatusUpdateRepository;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionRepositoryException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import java.util.List;

@Repository
public class InfrastructureStatusUpdateRepositoryImpl implements DomainOutputPortStatusUpdateRepository {

    private final InfrastructureJpaPaymentStatusUpdateRepository jpaRepository;

    public InfrastructureStatusUpdateRepositoryImpl(InfrastructureJpaPaymentStatusUpdateRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<DomainEntityPaymentStatusUpdate> findUnprocessedUpdates() {
        try {
            return jpaRepository.findByProcessedFalseOrderByUpdatedAt();
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findUnprocessedUpdates", 
                DomainEntityPaymentStatusUpdate.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    @Transactional
    public void markAsProcessed(Long id) {
        try {
            jpaRepository.updateProcessedById(id, true);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "markAsProcessed", 
                DomainEntityPaymentStatusUpdate.class.getSimpleName(), 
                e
            );
        }
    }
}