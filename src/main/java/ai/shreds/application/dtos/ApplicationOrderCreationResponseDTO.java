package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.domain.value_objects.DomainOrderAggregate;
import ai.shreds.domain.entities.DomainOrderEntity;
import java.time.Instant;
import java.time.ZoneOffset;

public class ApplicationOrderCreationResponseDTO {

    private String orderId;
    private String orderNumber;
    private SharedOrderStatusEnum status;
    private SharedMoneyDTO totalAmount;
    private Instant createdAt;

    public ApplicationOrderCreationResponseDTO() {
    }

    public ApplicationOrderCreationResponseDTO(String orderId,
                                                String orderNumber,
                                                SharedOrderStatusEnum status,
                                                SharedMoneyDTO totalAmount,
                                                Instant createdAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
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

    public SharedOrderStatusEnum getStatus() {
        return status;
    }

    public void setStatus(SharedOrderStatusEnum status) {
        this.status = status;
    }

    public SharedMoneyDTO getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(SharedMoneyDTO totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static ApplicationOrderCreationResponseDTO fromDomainOrder(DomainOrderAggregate aggregate) {
        DomainOrderEntity order = aggregate.getOrder();
        ApplicationOrderCreationResponseDTO dto = new ApplicationOrderCreationResponseDTO();
        dto.setOrderId(order.getOrderId().toString());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getOrderStatus());
        dto.setTotalAmount(new SharedMoneyDTO(order.getTotalAmount(), order.getCurrency()));
        dto.setCreatedAt(order.getOrderDate().atOffset(ZoneOffset.UTC).toInstant());
        return dto;
    }
}
