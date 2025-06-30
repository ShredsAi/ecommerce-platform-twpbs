package ai.shreds.domain.entities;

import ai.shreds.shared.dtos.SharedOutboxEvent;
import java.time.Instant;
import java.util.UUID;

public class DomainEntityOutboxEvent {
    private final UUID eventId;
    private final UUID aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String payload;
    private final Instant occurredOn;
    private boolean processed;
    private Instant processedOn;

    private DomainEntityOutboxEvent(UUID eventId,
                                   UUID aggregateId,
                                   String aggregateType,
                                   String eventType,
                                   String payload,
                                   Instant occurredOn,
                                   boolean processed,
                                   Instant processedOn) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredOn = occurredOn;
        this.processed = processed;
        this.processedOn = processedOn;
    }
    
    public static DomainEntityOutboxEvent create(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload) {
        return new DomainEntityOutboxEvent(
                UUID.randomUUID(),
                aggregateId,
                aggregateType,
                eventType,
                payload,
                Instant.now(),
                false,
                null
        );
    }
    
    public static DomainEntityOutboxEvent reconstruct(
            UUID eventId,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload,
            Instant occurredOn,
            boolean processed,
            Instant processedOn) {
        return new DomainEntityOutboxEvent(
                eventId,
                aggregateId,
                aggregateType,
                eventType,
                payload,
                occurredOn,
                processed,
                processedOn
        );
    }

    public DomainEntityOutboxEvent markProcessed() {
        this.processed = true;
        this.processedOn = Instant.now();
        return this;
    }

    public SharedOutboxEvent toSharedDTO() {
        return new SharedOutboxEvent(
                eventId.toString(),
                aggregateId.toString(),
                aggregateType,
                eventType,
                payload,
                occurredOn,
                processed,
                processedOn
        );
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public boolean isProcessed() {
        return processed;
    }

    public Instant getProcessedOn() {
        return processedOn;
    }
}
