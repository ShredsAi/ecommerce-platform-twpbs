package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPayment;
import ai.shreds.domain.ports.DomainOutputPortPaymentQuery;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionRepositoryException;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;
import java.util.UUID;

@Repository
public class InfrastructurePaymentQueryImpl implements DomainOutputPortPaymentQuery {

    private final InfrastructureJpaPaymentRepository jpaRepository;

    public InfrastructurePaymentQueryImpl(InfrastructureJpaPaymentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DomainEntityPayment findPaymentByProcessorTransactionId(String processorTransactionId) {
        try {
            return jpaRepository.findByProcessorTransactionId(processorTransactionId)
                .orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findPaymentByProcessorTransactionId", 
                DomainEntityPayment.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    public DomainEntityPayment findPaymentById(UUID paymentId) {
        try {
            return jpaRepository.findById(paymentId)
                .orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findPaymentById", 
                DomainEntityPayment.class.getSimpleName(), 
                e
            );
        }
    }
}