package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.dtos.SharedOrderDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate root representing an Order in the domain.
 */
public class DomainOrderEntity {

    private final DomainOrderIdValue orderId;
    private final DomainCustomerIdValue customerId;
    private String orderNumber;
    private SharedOrderStatusEnum orderStatus;
    private LocalDateTime orderDate;
    private SharedMoneyValue totalAmount;
    private SharedMoneyValue subtotalAmount;
    private List<DomainOrderItemEntity> orderItems;
    private final SharedAddressValue billingAddress;
    private final SharedAddressValue shippingAddress;
    private final Instant createdAt;
    private Instant updatedAt;
    private int version;
    private final List<DomainOrderEventEntity> orderEvents = new ArrayList<>();

    /**
     * All-args constructor made public for MapStruct instantiation.
     */
    public DomainOrderEntity(DomainOrderIdValue orderId,
                             DomainCustomerIdValue customerId,
                             String orderNumber,
                             SharedOrderStatusEnum orderStatus,
                             LocalDateTime orderDate,
                             SharedMoneyValue totalAmount,
                             SharedMoneyValue subtotalAmount,
                             List<DomainOrderItemEntity> orderItems,
                             SharedAddressValue billingAddress,
                             SharedAddressValue shippingAddress,
                             Instant createdAt,
                             Instant updatedAt,
                             int version) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.subtotalAmount = subtotalAmount;
        this.orderItems = orderItems;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Factory method to create a DomainOrderEntity from shared DTOs and required domain values.
     */
    public static DomainOrderEntity create(DomainOrderIdValue orderId,
                                          DomainCustomerIdValue customerId,
                                          String orderNumber,
                                          String status,
                                          SharedMoneyValue totalAmount,
                                          List<SharedOrderItemDTO> items,
                                          SharedAddressValue billingAddress,
                                          SharedAddressValue shippingAddress) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        
        SharedOrderStatusEnum orderStatus = SharedOrderStatusEnum.valueOf(status);

        // Map items
        List<DomainOrderItemEntity> orderItems = items.stream()
            .map(DomainOrderItemEntity::fromSharedDTO)
            .collect(Collectors.toList());
            
        // Calculate subtotal
        String currency = items.isEmpty() ? totalAmount.getCurrency() : items.get(0).getUnitPrice().getCurrency();
        var subTotalValue = orderItems.stream()
            .map(i -> i.getTotalPrice().getValue())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        SharedMoneyValue subtotal = new SharedMoneyValue(subTotalValue, currency);
        
        Instant now = Instant.now();
        LocalDateTime orderDate = LocalDateTime.now();
        
        return new DomainOrderEntity(
            orderId,
            customerId,
            orderNumber,
            orderStatus,
            orderDate,
            totalAmount,
            subtotal,
            orderItems,
            billingAddress,
            shippingAddress,
            now,
            now,
            0
        );
    }

    /**
     * Factory method to create a DomainOrderEntity from a SharedOrderDTO and additional required values.
     */
    public static DomainOrderEntity fromSharedDTO(SharedOrderDTO dto, 
                                                 SharedAddressValue billingAddress, 
                                                 SharedAddressValue shippingAddress) {
        DomainOrderIdValue orderId = new DomainOrderIdValue(dto.getOrderId());
        DomainCustomerIdValue customerId = new DomainCustomerIdValue(dto.getCustomerId());
        SharedOrderStatusEnum status = SharedOrderStatusEnum.valueOf(dto.getOrderStatus());
        
        List<DomainOrderItemEntity> items = dto.getItems().stream()
            .map(DomainOrderItemEntity::fromSharedDTO)
            .collect(Collectors.toList());
        
        // Calculate subtotal if not present
        SharedMoneyValue subtotal;
        if (items.isEmpty()) {
            subtotal = new SharedMoneyValue(java.math.BigDecimal.ZERO, dto.getTotalAmount().getCurrency());
        } else {
            var subTotalValue = items.stream()
                .map(i -> i.getTotalPrice().getValue())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            subtotal = new SharedMoneyValue(subTotalValue, items.get(0).getTotalPrice().getCurrency());
        }
        
        Instant now = Instant.now();
        
        return new DomainOrderEntity(
            orderId,
            customerId,
            dto.getOrderNumber(),
            status,
            LocalDateTime.now(),
            dto.getTotalAmount(),
            subtotal,
            items,
            billingAddress,
            shippingAddress,
            now,
            now,
            0
        );
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public DomainCustomerIdValue getCustomerId() {
        return customerId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public SharedOrderStatusEnum getOrderStatus() {
        return orderStatus;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public SharedMoneyValue getTotalAmount() {
        return totalAmount;
    }

    public SharedMoneyValue getSubtotalAmount() {
        return subtotalAmount;
    }

    public List<DomainOrderItemEntity> getOrderItems() {
        return orderItems;
    }

    public SharedAddressValue getBillingAddress() {
        return billingAddress;
    }

    public SharedAddressValue getShippingAddress() {
        return shippingAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public List<DomainOrderEventEntity> getOrderEvents() {
        return orderEvents;
    }

    /**
     * Transition the order to a new status and update timestamp.
     * @param newStatus The new status to transition to
     */
    public void transitionTo(SharedOrderStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        SharedOrderStatusEnum previousStatus = this.orderStatus;
        this.orderStatus = newStatus;
        this.updatedAt = Instant.now();
        
        // Create an event for this transition
        DomainOrderEventEntity event = DomainOrderEventEntity.createStatusChangeEvent(
            this.orderId.getValue(), 
            previousStatus, 
            newStatus
        );
        addOrderEvent(event);
    }

    /**
     * Add an event to the order's history.
     * @param event The event to add
     */
    public void addOrderEvent(DomainOrderEventEntity event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        this.orderEvents.add(event);
    }

    /**
     * Update the order items and recalculate totals.
     * @param newItems The new list of order items
     */
    public void updateOrderItems(List<DomainOrderItemEntity> newItems) {
        if (newItems == null) {
            throw new IllegalArgumentException("Order items list cannot be null");
        }
        
        this.orderItems = newItems;
        
        // Recalculate subtotal
        if (!newItems.isEmpty()) {
            String currency = newItems.get(0).getTotalPrice().getCurrency();
            var subTotalValue = newItems.stream()
                .map(i -> i.getTotalPrice().getValue())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            this.subtotalAmount = new SharedMoneyValue(subTotalValue, currency);
        }
        
        this.updatedAt = Instant.now();
    }

    /**
     * Convert this domain entity to a shared DTO for external use.
     */
    public SharedOrderDTO toSharedDTO() {
        SharedOrderDTO dto = new SharedOrderDTO();
        dto.setOrderId(this.orderId.getValue());
        dto.setCustomerId(this.customerId.getValue());
        dto.setOrderNumber(this.orderNumber);
        dto.setOrderStatus(this.orderStatus.name());
        dto.setTotalAmount(this.totalAmount);
        dto.setItems(this.orderItems.stream()
            .map(DomainOrderItemEntity::toSharedDTO)
            .collect(Collectors.toList()));
        return dto;
    }
}