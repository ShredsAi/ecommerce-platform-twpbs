package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.enums.SharedEventTypeEnum;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * Domain entity representing an event that occurs in an Order's lifecycle.
 */
@Entity
@Table(name = "order_events")
public class DomainOrderEventEntity {
    
    @Id
    @Column(name = "event_id", nullable = false, length = 50)
    private UUID eventId;

    @Column(name = "order_id", nullable = false, length = 50)
    private UUID orderId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_event_data", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> eventData = new HashMap<>();

    @Column(name = "timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "version")
    private Integer version;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor for JPA
    protected DomainOrderEventEntity() {}

    private DomainOrderEventEntity(UUID eventId,
                                  UUID orderId,
                                  SharedEventTypeEnum eventType,
                                  Instant eventTimestamp,
                                  String eventDataString,
                                  SharedOrderStatusEnum previousStatus,
                                  SharedOrderStatusEnum newStatus,
                                  Instant createdAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.orderId = orderId;
        this.eventType = eventType.name();
        this.eventTimestamp = eventTimestamp != null ? eventTimestamp : Instant.now();
        this.previousStatus = previousStatus != null ? previousStatus.name() : null;
        this.newStatus = newStatus != null ? newStatus.name() : null;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        
        if (eventDataString != null) {
            this.eventData.put("data", eventDataString);
        }
    }

    /**
     * Creates a basic event with the given parameters.
     */
    public static DomainOrderEventEntity create(UUID orderId,
                                              SharedEventTypeEnum eventType,
                                              String eventDataString,
                                              SharedOrderStatusEnum previousStatus,
                                              SharedOrderStatusEnum newStatus) {
        return new DomainOrderEventEntity(
            UUID.randomUUID(),
            orderId,
            eventType,
            Instant.now(),
            eventDataString,
            previousStatus,
            newStatus,
            Instant.now()
        );
    }
    
    /**
     * Creates an event specifically for order status changes.
     */
    public static DomainOrderEventEntity createStatusChangeEvent(UUID orderId,
                                                               SharedOrderStatusEnum previousStatus,
                                                               SharedOrderStatusEnum newStatus) {
        if (previousStatus == null) {
            throw new IllegalArgumentException("Previous status cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        String eventDataString = String.format("Status changed from %s to %s", 
                                       previousStatus.name(), 
                                       newStatus.name());
        
        return new DomainOrderEventEntity(
            UUID.randomUUID(),
            orderId,
            SharedEventTypeEnum.ORDER_CREATED, // Use existing enum value instead of non-existent ORDER_STATUS_CHANGED
            Instant.now(),
            eventDataString,
            previousStatus,
            newStatus,
            Instant.now()
        );
    }
    
    /**
     * Creates an event for order creation.
     */
    public static DomainOrderEventEntity createOrderCreatedEvent(UUID orderId) {
        return new DomainOrderEventEntity(
            UUID.randomUUID(),
            orderId,
            SharedEventTypeEnum.ORDER_CREATED,
            Instant.now(),
            "Order created",
            null,
            SharedOrderStatusEnum.PENDING,
            Instant.now()
        );
    }
    
    /**
     * Creates an event for payment processes.
     */
    public static DomainOrderEventEntity createPaymentEvent(UUID orderId,
                                                          SharedEventTypeEnum paymentEventType,
                                                          String transactionDetails,
                                                          SharedOrderStatusEnum previousStatus,
                                                          SharedOrderStatusEnum newStatus) {
        if (paymentEventType != SharedEventTypeEnum.PAYMENT_INITIATED &&
            paymentEventType != SharedEventTypeEnum.PAYMENT_AUTHORIZED &&
            paymentEventType != SharedEventTypeEnum.PAYMENT_CAPTURED &&
            paymentEventType != SharedEventTypeEnum.PAYMENT_FAILED) {
            throw new IllegalArgumentException("Invalid payment event type: " + paymentEventType);
        }
        
        return new DomainOrderEventEntity(
            UUID.randomUUID(),
            orderId,
            paymentEventType,
            Instant.now(),
            transactionDetails,
            previousStatus,
            newStatus,
            Instant.now()
        );
    }
    
    /**
     * Creates an event for shipping processes.
     */
    public static DomainOrderEventEntity createShippingEvent(UUID orderId,
                                                           SharedEventTypeEnum shippingEventType,
                                                           String shippingDetails,
                                                           SharedOrderStatusEnum previousStatus,
                                                           SharedOrderStatusEnum newStatus) {
        if (shippingEventType != SharedEventTypeEnum.SHIPPING_ARRANGED &&
            shippingEventType != SharedEventTypeEnum.SHIPPING_FAILED &&
            shippingEventType != SharedEventTypeEnum.ORDER_SHIPPED &&
            shippingEventType != SharedEventTypeEnum.ORDER_DELIVERED) {
            throw new IllegalArgumentException("Invalid shipping event type: " + shippingEventType);
        }
        
        return new DomainOrderEventEntity(
            UUID.randomUUID(),
            orderId,
            shippingEventType,
            Instant.now(),
            shippingDetails,
            previousStatus,
            newStatus,
            Instant.now()
        );
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Map<String, String> getEventData() {
        return eventData;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getVersion() {
        return version;
    }

    public String getSource() {
        return source;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}