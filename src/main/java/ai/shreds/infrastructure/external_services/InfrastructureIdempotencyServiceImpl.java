package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortIdempotencyService;
import ai.shreds.infrastructure.repositories.InfrastructureJpaPaymentWebhookRepository;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InfrastructureIdempotencyServiceImpl implements DomainOutputPortIdempotencyService {

    private final InfrastructureJpaPaymentWebhookRepository webhookRepository;

    public InfrastructureIdempotencyServiceImpl(InfrastructureJpaPaymentWebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicate(String externalEventId, SharedEnumPaymentProcessorType processorType) {
        return webhookRepository.findByExternalEventIdAndProcessorType(
            externalEventId,
            processorType.name()
        ).isPresent();
    }
}
