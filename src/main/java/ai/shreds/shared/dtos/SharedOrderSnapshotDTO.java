package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order snapshot data transfer.
 */
public class SharedOrderSnapshotDTO {
    
    private String orderId;
    private String customerId;
    private String orderStatus;
    private LocalDateTime orderDate;
    private SharedMoneyValue totalAmount;
    private List<SharedOrderItemDTO> items;
    private String paymentStatus;
    private String shippingStatus;
    private LocalDateTime deliveryDate;
    
    // Default constructor
    public SharedOrderSnapshotDTO() {}
    
    // All-args constructor
    public SharedOrderSnapshotDTO(String orderId, String customerId, String orderStatus,
                                 LocalDateTime orderDate, SharedMoneyValue totalAmount,
                                 List<SharedOrderItemDTO> items, String paymentStatus,
                                 String shippingStatus, LocalDateTime deliveryDate) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.items = items;
        this.paymentStatus = paymentStatus;
        this.shippingStatus = shippingStatus;
        this.deliveryDate = deliveryDate;
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    public SharedMoneyValue getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(SharedMoneyValue totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public List<SharedOrderItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<SharedOrderItemDTO> items) {
        this.items = items;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getShippingStatus() {
        return shippingStatus;
    }
    
    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }
    
    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}