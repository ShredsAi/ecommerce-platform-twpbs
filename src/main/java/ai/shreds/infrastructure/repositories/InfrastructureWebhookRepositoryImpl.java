package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.ports.DomainOutputPortWebhookRepository;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionRepositoryException;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InfrastructureWebhookRepositoryImpl implements DomainOutputPortWebhookRepository {

    private final InfrastructureJpaPaymentWebhookRepository jpaRepository;

    public InfrastructureWebhookRepositoryImpl(InfrastructureJpaPaymentWebhookRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DomainEntityPaymentWebhook save(DomainEntityPaymentWebhook webhook) {
        try {
            return jpaRepository.save(webhook);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "save", 
                DomainEntityPaymentWebhook.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    public DomainEntityPaymentWebhook findById(UUID id) {
        try {
            Optional<DomainEntityPaymentWebhook> result = jpaRepository.findById(id);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findById", 
                DomainEntityPaymentWebhook.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    public DomainEntityPaymentWebhook findByExternalEventIdAndProcessorType(
            String externalEventId, 
            SharedEnumPaymentProcessorType processorType) {
        try {
            String processorTypeString = processorType.name();
            Optional<DomainEntityPaymentWebhook> result = jpaRepository
                .findByExternalEventIdAndProcessorType(externalEventId, processorTypeString);
            return result.orElse(null);
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findByExternalEventIdAndProcessorType", 
                DomainEntityPaymentWebhook.class.getSimpleName(), 
                e
            );
        }
    }

    @Override
    public List<DomainEntityPaymentWebhook> findPendingWebhooks() {
        try {
            return jpaRepository.findByProcessingStatus("PENDING");
        } catch (DataAccessException e) {
            throw new InfrastructureExceptionRepositoryException(
                "findPendingWebhooks", 
                DomainEntityPaymentWebhook.class.getSimpleName(), 
                e
            );
        }
    }
}