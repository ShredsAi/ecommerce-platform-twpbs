package ai.shreds.domain.events;

import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import ai.shreds.shared.enums.SharedErrorTypeEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing failed order creation.
 */
@Getter
@Builder
public class DomainOrderCreationFailedEvent {
    private final String cartId;
    private final String customerId;
    private final SharedErrorTypeEnum errorType;
    private final String errorMessage;
    private final String failureReason;
    private final Instant occurredOn;
    private final String correlationId;

    public DomainOrderCreationFailedEvent(String cartId, 
                                         String customerId, 
                                         SharedErrorTypeEnum errorType, 
                                         String errorMessage, 
                                         String failureReason, 
                                         Instant occurredOn,
                                         String correlationId) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.failureReason = failureReason;
        this.occurredOn = occurredOn != null ? occurredOn : Instant.now();
        this.correlationId = correlationId;
    }

    /**
     * Converts this domain event to a shared DTO for external communication.
     *
     * @return the shared DTO representation
     */
    public SharedOrderCreationFailedEventDTO toSharedDTO() {
        return SharedOrderCreationFailedEventDTO.builder()
            .cartId(cartId)
            .customerId(customerId)
            .errorType(errorType)
            .errorMessage(errorMessage)
            .failureReason(failureReason)
            .timestamp(occurredOn)
            .correlationId(correlationId)
            .eventId(UUID.randomUUID().toString())
            .occurredOn(occurredOn)
            .build();
    }
}