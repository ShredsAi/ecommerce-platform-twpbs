package ai.shreds.shared.dtos;

import java.util.List;

public class SharedOrderSnapshotDTO {
    private String orderId;
    private String orderNumber;
    private String orderStatus;
    private List<?> items;
    private Object totalAmount;
    private Object shippingAddress;
    private String customerId;
    private String source;
    private Object metadata;

    public SharedOrderSnapshotDTO() {
    }

    public SharedOrderSnapshotDTO(String orderId, String orderNumber, String orderStatus,
                                  List<?> items, Object totalAmount, Object shippingAddress,
                                  String customerId, String source, Object metadata) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.items = items;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.customerId = customerId;
        this.source = source;
        this.metadata = metadata;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public List<?> getItems() {
        return items;
    }

    public void setItems(List<?> items) {
        this.items = items;
    }

    public Object getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Object totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Object getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Object shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}