package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import ai.shreds.domain.ports.DomainOutputPortCorrelationService;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionRepositoryException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import java.util.UUID;

@Service
public class InfrastructureCorrelationServiceImpl implements DomainOutputPortCorrelationService {

    private final InfrastructureJpaPaymentWebhookCorrelationRepository jpaRepository;

    public InfrastructureCorrelationServiceImpl(InfrastructureJpaPaymentWebhookCorrelationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void saveCorrelation(DomainEntityPaymentWebhookCorrelation correlation) {
        try {
            jpaRepository.save(correlation);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "saveCorrelation", 
                DomainEntityPaymentWebhookCorrelation.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    public DomainEntityPaymentWebhookCorrelation findCorrelationByWebhookId(UUID webhookId) {
        try {
            return jpaRepository.findByWebhookId(webhookId)
                .orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findCorrelationByWebhookId", 
                DomainEntityPaymentWebhookCorrelation.class.getSimpleName(), 
                e
            );
        }
    }
}