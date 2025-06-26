package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import ai.shreds.domain.ports.DomainOutputPortOrderEventRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain service for managing order events.
 * Handles the creation, persistence, and processing of order-related events.
 */
@Service
public class DomainOrderEventService {
    
    private final DomainOutputPortOrderEventRepository eventRepository;
    private final DomainOutputPortOrderRepository orderRepository;
    
    public DomainOrderEventService(
        DomainOutputPortOrderEventRepository eventRepository,
        DomainOutputPortOrderRepository orderRepository
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "Event repository cannot be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository cannot be null");
    }
    
    /**
     * Record a new order event.
     * 
     * @param orderId the ID of the order
     * @param eventType the type of event
     * @param eventData additional data associated with the event
     * @return the created order event entity
     */
    public DomainOrderEventEntity recordEvent(String orderId, String eventType, Map<String, Object> eventData) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(eventType, "Event type cannot be null");
        
        String eventId = generateEventId();
        String source = "ORDER_CANCELLATION_RETURNS_SERVICE";
        
        DomainOrderEventEntity event = new DomainOrderEventEntity(
            eventId,
            orderId,
            eventType,
            eventData,
            source
        );
        
        return eventRepository.save(event);
    }
    
    /**
     * Process an order status change event.
     * 
     * @param orderId the ID of the order
     * @param oldStatus the previous status
     * @param newStatus the new status
     */
    public void processOrderStatusChange(String orderId, String oldStatus, String newStatus) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(newStatus, "New status cannot be null");
        
        // Create event data
        Map<String, Object> eventData = Map.of(
            "old_status", oldStatus != null ? oldStatus : "UNKNOWN",
            "new_status", newStatus,
            "changed_at", LocalDateTime.now(),
            "source", "ORDER_STATUS_UPDATE"
        );
        
        // Record the event
        recordEvent(orderId, DomainOrderEventEntity.EVENT_TYPE_ORDER_UPDATED, eventData);
        
        // Update the order status in the external system
        try {
            orderRepository.updateOrderStatus(orderId, newStatus);
        } catch (Exception e) {
            // Log error but don't fail the event recording
            // In a real implementation, this might trigger compensation or retry logic
            recordEvent(orderId, "ORDER_STATUS_UPDATE_FAILED", Map.of(
                "attempted_status", newStatus,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    /**
     * Get the history of events for an order.
     * 
     * @param orderId the ID of the order
     * @return list of order events, typically ordered by timestamp
     */
    public List<DomainOrderEventEntity> getOrderHistory(String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        return eventRepository.findByOrderId(orderId);
    }
    
    /**
     * Record a cancellation event.
     * 
     * @param orderId the ID of the order
     * @param cancellationId the ID of the cancellation
     * @param reason the cancellation reason
     * @return the created event entity
     */
    public DomainOrderEventEntity recordCancellationEvent(String orderId, String cancellationId, String reason) {
        Map<String, Object> eventData = Map.of(
            "cancellation_id", cancellationId,
            "reason", reason,
            "timestamp", LocalDateTime.now()
        );
        
        return recordEvent(orderId, DomainOrderEventEntity.EVENT_TYPE_ORDER_CANCELLED, eventData);
    }
    
    /**
     * Record a return event.
     * 
     * @param orderId the ID of the order
     * @param returnId the ID of the return
     * @param rmaNumber the RMA number
     * @return the created event entity
     */
    public DomainOrderEventEntity recordReturnEvent(String orderId, String returnId, String rmaNumber) {
        Map<String, Object> eventData = Map.of(
            "return_id", returnId,
            "rma_number", rmaNumber,
            "timestamp", LocalDateTime.now()
        );
        
        return recordEvent(orderId, DomainOrderEventEntity.EVENT_TYPE_RETURN_REQUESTED, eventData);
    }
    
    /**
     * Record a refund processed event.
     * 
     * @param orderId the ID of the order
     * @param refundId the ID of the refund
     * @param amount the refund amount
     * @return the created event entity
     */
    public DomainOrderEventEntity recordRefundEvent(String orderId, String refundId, String amount) {
        Map<String, Object> eventData = Map.of(
            "refund_id", refundId,
            "amount", amount,
            "timestamp", LocalDateTime.now()
        );
        
        return recordEvent(orderId, DomainOrderEventEntity.EVENT_TYPE_REFUND_PROCESSED, eventData);
    }
    
    /**
     * Generate a unique event ID.
     */
    private String generateEventId() {
        return "EVT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}