package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing successful order creation.
 */
@Getter
@Builder
public class DomainOrderCreatedEvent {
    private final DomainOrderIdValue orderId;
    private final String orderNumber;
    private final DomainCustomerIdValue customerId;
    private final DomainMoneyValue totalAmount;
    private final Integer itemCount;
    private final Instant occurredOn;
    private final String correlationId;

    public DomainOrderCreatedEvent(DomainOrderIdValue orderId, 
                                  String orderNumber, 
                                  DomainCustomerIdValue customerId, 
                                  DomainMoneyValue totalAmount, 
                                  Integer itemCount, 
                                  Instant occurredOn,
                                  String correlationId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.occurredOn = occurredOn != null ? occurredOn : Instant.now();
        this.correlationId = correlationId;
    }

    /**
     * Converts this domain event to a shared DTO for external communication.
     *
     * @return the shared DTO representation
     */
    public SharedOrderCreatedEventDTO toSharedDTO() {
        return SharedOrderCreatedEventDTO.builder()
            .orderId(orderId.getValue().toString())
            .orderNumber(orderNumber)
            .customerId(customerId.getValue())
            .orderStatus(ai.shreds.shared.enums.SharedOrderStatusEnum.PENDING)
            .totalAmount(SharedMoneyDTO.builder()
                .amount(totalAmount.getAmount())
                .currency(totalAmount.getCurrency())
                .build())
            .itemCount(itemCount)
            .timestamp(occurredOn)
            .correlationId(correlationId)
            .eventId(UUID.randomUUID().toString())
            .occurredOn(occurredOn)
            .build();
    }
}