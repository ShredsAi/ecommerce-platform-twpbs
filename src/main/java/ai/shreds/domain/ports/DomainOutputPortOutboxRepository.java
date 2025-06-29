package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import java.util.List;
import java.util.UUID;

public interface DomainOutputPortOutboxRepository {
    DomainEntityOutboxEvent save(DomainEntityOutboxEvent event);
    List<DomainEntityOutboxEvent> fetchUnprocessed(int batchSize);
    void markProcessed(UUID eventId);
}
