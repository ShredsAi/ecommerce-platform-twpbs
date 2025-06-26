package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Repository;

import order.OrderServiceGrpc;
import order.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of DomainOutputPortOrderRepository using gRPC.
 */
@Repository
public class InfrastructureOrderRepositoryImpl implements DomainOutputPortOrderRepository {

    private final OrderServiceGrpc.OrderServiceBlockingStub grpcStub;

    public InfrastructureOrderRepositoryImpl(OrderServiceGrpc.OrderServiceBlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "fallbackFindOrderSnapshot")
    public SharedOrderSnapshotDTO findOrderSnapshot(String orderId) {
        try {
            Order.GetOrderRequest request = Order.GetOrderRequest.newBuilder()
                    .setOrderId(orderId)
                    .build();
            Order.OrderSnapshot response = grpcStub.getOrderSnapshot(request);
            return mapGrpcSnapshot(response);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("OrderService", ex.getMessage(), ex);
        }
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "fallbackUpdateOrderStatus")
    public void updateOrderStatus(String orderId, String newStatus) {
        try {
            Order.UpdateStatusRequest request = Order.UpdateStatusRequest.newBuilder()
                    .setOrderId(orderId)
                    .setNewStatus(newStatus)
                    .build();
            grpcStub.updateOrderStatus(request);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("OrderService", ex.getMessage(), ex);
        }
    }

    public SharedOrderSnapshotDTO mapGrpcSnapshot(Order.OrderSnapshot response) {
        SharedOrderSnapshotDTO dto = new SharedOrderSnapshotDTO();
        dto.setOrderId(response.getOrderId());
        dto.setCustomerId(response.getCustomerId());
        dto.setOrderStatus(response.getOrderStatus());
        
        // Convert order date string to LocalDateTime if present
        if (response.getOrderDate() != null && !response.getOrderDate().isEmpty()) {
            try {
                dto.setOrderDate(LocalDateTime.parse(response.getOrderDate()));
            } catch (Exception e) {
                // Handle date parsing error gracefully
                dto.setOrderDate(LocalDateTime.now());
            }
        }
        
        // Convert delivery date string to LocalDateTime if present
        if (response.getDeliveryDate() != null && !response.getDeliveryDate().isEmpty()) {
            try {
                dto.setDeliveryDate(LocalDateTime.parse(response.getDeliveryDate()));
            } catch (Exception e) {
                // Handle date parsing error gracefully
                dto.setDeliveryDate(null);
            }
        }
        
        // Map money value
        if (response.hasTotalAmount()) {
            Order.Money totalAmount = response.getTotalAmount();
            dto.setTotalAmount(new SharedMoneyValue(
                new BigDecimal(totalAmount.getAmount()),
                totalAmount.getCurrency()
            ));
        }
        
        // Map payment and shipping status
        dto.setPaymentStatus(response.getPaymentStatus());
        dto.setShippingStatus(response.getShippingStatus());
        
        // Map order items
        List<SharedOrderItemDTO> items = new ArrayList<>();
        for (Order.OrderItem item : response.getItemsList()) {
            SharedOrderItemDTO itemDto = new SharedOrderItemDTO();
            itemDto.setOrderItemId(item.getOrderItemId());
            itemDto.setProductId(item.getProductId());
            itemDto.setProductName(item.getProductName());
            itemDto.setQuantity(item.getQuantity());
            
            // Map unit price
            if (item.hasUnitPrice()) {
                Order.Money unitPrice = item.getUnitPrice();
                itemDto.setUnitPrice(new SharedMoneyValue(
                    new BigDecimal(unitPrice.getAmount()),
                    unitPrice.getCurrency()
                ));
            }
            
            // Map total price
            if (item.hasTotalPrice()) {
                Order.Money totalPrice = item.getTotalPrice();
                itemDto.setTotalPrice(new SharedMoneyValue(
                    new BigDecimal(totalPrice.getAmount()),
                    totalPrice.getCurrency()
                ));
            }
            
            itemDto.setIsReturnable(item.getIsReturnable());
            items.add(itemDto);
        }
        dto.setItems(items);
        
        return dto;
    }

    @SuppressWarnings("unused")
    private SharedOrderSnapshotDTO fallbackFindOrderSnapshot(String orderId, Throwable ex) {
        throw new InfrastructureExternalServiceException("OrderService", "Fallback failed: " + ex.getMessage(), ex);
    }

    @SuppressWarnings("unused")
    private void fallbackUpdateOrderStatus(String orderId, String newStatus, Throwable ex) {
        throw new InfrastructureExternalServiceException("OrderService", "Fallback failed: " + ex.getMessage(), ex);
    }
}