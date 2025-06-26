package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.enums.SharedShippingStatusEnum;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Domain entity representing shipping details for an order.
 */
public class DomainShippingDetailsEntity {
    private final DomainOrderIdValue orderId;
    private SharedShippingStatusEnum shippingStatus;
    private String trackingNumber;
    private String carrier;
    private Date estimatedDeliveryDate;
    private Date actualDeliveryDate;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * All-args constructor made public for MapStruct instantiation.
     */
    public DomainShippingDetailsEntity(DomainOrderIdValue orderId,
                                        SharedShippingStatusEnum shippingStatus,
                                        String trackingNumber,
                                        String carrier,
                                        Date estimatedDeliveryDate,
                                        Date actualDeliveryDate,
                                        Instant createdAt,
                                        Instant updatedAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (shippingStatus == null) {
            throw new IllegalArgumentException("Shipping status cannot be null");
        }
        
        this.orderId = orderId;
        this.shippingStatus = shippingStatus;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.actualDeliveryDate = actualDeliveryDate;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }
    
    /**
     * Factory method to create shipping details for a new shipment.
     */
    public static DomainShippingDetailsEntity create(DomainOrderIdValue orderId,
                                                   String trackingNumber,
                                                   String carrier,
                                                   Date estimatedDeliveryDate) {
        return new DomainShippingDetailsEntity(
            orderId,
            SharedShippingStatusEnum.PENDING,
            trackingNumber,
            carrier,
            estimatedDeliveryDate,
            null,
            Instant.now(),
            Instant.now()
        );
    }
    
    /**
     * Factory method to create shipping details from shared data.
     */
    public static DomainShippingDetailsEntity fromSharedData(UUID orderId,
                                                           String status,
                                                           String trackingNumber,
                                                           String carrier,
                                                           Date estimatedDeliveryDate,
                                                           Date actualDeliveryDate) {
        DomainOrderIdValue orderIdValue = new DomainOrderIdValue(orderId);
        SharedShippingStatusEnum shippingStatus = SharedShippingStatusEnum.valueOf(status);
        
        return new DomainShippingDetailsEntity(
            orderIdValue,
            shippingStatus,
            trackingNumber,
            carrier,
            estimatedDeliveryDate,
            actualDeliveryDate,
            Instant.now(),
            Instant.now()
        );
    }
    
    /**
     * Factory method to create shipping details when shipment is arranged.
     */
    public static DomainShippingDetailsEntity createArranged(DomainOrderIdValue orderId,
                                                           String trackingNumber,
                                                           String carrier,
                                                           Date estimatedDeliveryDate) {
        return new DomainShippingDetailsEntity(
            orderId,
            SharedShippingStatusEnum.ARRANGED,
            trackingNumber,
            carrier,
            estimatedDeliveryDate,
            null,
            Instant.now(),
            Instant.now()
        );
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public SharedShippingStatusEnum getShippingStatus() {
        return shippingStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public Date getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public Date getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Update the shipping status and timestamp.
     * @param newStatus The new shipping status
     */
    public void updateStatus(SharedShippingStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        this.shippingStatus = newStatus;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update shipping details with new information.
     */
    public void updateShippingInfo(String trackingNumber, 
                                 String carrier, 
                                 Date estimatedDeliveryDate) {
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            this.trackingNumber = trackingNumber;
        }
        if (carrier != null && !carrier.isBlank()) {
            this.carrier = carrier;
        }
        if (estimatedDeliveryDate != null) {
            this.estimatedDeliveryDate = estimatedDeliveryDate;
        }
        this.updatedAt = Instant.now();
    }
    
    /**
     * Mark the shipment as delivered with actual delivery date.
     */
    public void markAsDelivered(Date actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate != null ? actualDeliveryDate : new Date();
        this.shippingStatus = SharedShippingStatusEnum.DELIVERED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if the shipment is in a final state.
     * @return true if shipment is delivered or failed
     */
    public boolean isInFinalState() {
        return shippingStatus == SharedShippingStatusEnum.DELIVERED ||
               shippingStatus == SharedShippingStatusEnum.FAILED;
    }
    
    /**
     * Check if the shipment can be cancelled.
     * @return true if shipment can be cancelled
     */
    public boolean canBeCancelled() {
        return shippingStatus == SharedShippingStatusEnum.PENDING ||
               shippingStatus == SharedShippingStatusEnum.ARRANGED;
    }
}