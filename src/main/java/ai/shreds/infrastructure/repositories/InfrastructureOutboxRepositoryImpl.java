package ai.shreds.infrastructure.repositories;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.infrastructure.entities.InfrastructureJpaEntityOutboxEvent;
import ai.shreds.infrastructure.converters.InfrastructureEntityMapper;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabaseError;

@Repository
public class InfrastructureOutboxRepositoryImpl implements DomainOutputPortOutboxRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureOutboxRepositoryImpl.class);

    private final InfrastructureJpaOutboxEventRepository jpaRepository;
    private final InfrastructureEntityMapper entityMapper;

    public InfrastructureOutboxRepositoryImpl(InfrastructureJpaOutboxEventRepository jpaRepository, InfrastructureEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public DomainEntityOutboxEvent save(DomainEntityOutboxEvent event) {
        try {
            log.debug("Saving outbox event: {}, aggregate: {}, type: {}", 
                event.getEventId(), event.getAggregateId(), event.getEventType());
                
            InfrastructureJpaEntityOutboxEvent jpaEntity = entityMapper.toJpaEntity(event);
            InfrastructureJpaEntityOutboxEvent saved = jpaRepository.save(jpaEntity);
            
            log.debug("Successfully saved outbox event: {}", saved.getEventId());
            return entityMapper.toDomainEntity(saved);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new InfrastructureExceptionDatabaseError("Failed to save outbox event: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DomainEntityOutboxEvent> fetchUnprocessed(int batchSize) {
        try {
            log.debug("Fetching up to {} unprocessed outbox events", batchSize);
            
            List<InfrastructureJpaEntityOutboxEvent> entities;
            if (batchSize > 0) {
                entities = jpaRepository.findTopByProcessedFalseOrderByOccurredOn(batchSize);
            } else {
                entities = jpaRepository.findTop100ByProcessedFalseOrderByOccurredOn();
            }
            
            List<DomainEntityOutboxEvent> domainEvents = entities.stream()
                .map(entityMapper::toDomainEntity)
                .collect(Collectors.toList());
                
            log.debug("Fetched {} unprocessed outbox events", domainEvents.size());
            return domainEvents;
        } catch (Exception e) {
            log.error("Failed to fetch unprocessed outbox events", e);
            throw new InfrastructureExceptionDatabaseError("Failed to fetch unprocessed outbox events: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void markProcessed(UUID eventId) {
        try {
            log.debug("Marking outbox event as processed: {}", eventId);
            
            InfrastructureJpaEntityOutboxEvent entity = jpaRepository.findById(eventId)
                .orElseThrow(() -> new DomainExceptionEntityNotFound("OutboxEvent", eventId.toString()));
                
            entity.setProcessed(true);
            entity.setProcessedOn(Instant.now());
            jpaRepository.save(entity);
            
            log.debug("Successfully marked outbox event as processed: {}", eventId);
        } catch (DomainExceptionEntityNotFound e) {
            log.warn("Outbox event not found for marking as processed: {}", eventId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to mark outbox event as processed: {}", eventId, e);
            throw new InfrastructureExceptionDatabaseError("Failed to mark outbox event as processed: " + e.getMessage(), e);
        }
    }
}
