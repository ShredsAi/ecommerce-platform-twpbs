package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationInventoryServiceOutputPort;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedReturnRequestDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import inventory.InventoryServiceGrpc;
import inventory.Inventory.CheckStockRequest;
import inventory.Inventory.CheckStockResponse;
import inventory.Inventory.ReleaseStockRequest;
import inventory.Inventory.StockItem;
import inventory.Inventory.IncrementStockRequest;

import java.util.stream.Collectors;
import java.util.List;

/**
 * Client for Inventory Service, implements resilient stock release and increment operations.
 */
@Service
public class InfrastructureInventoryServiceClient implements ApplicationInventoryServiceOutputPort {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub grpcStub;

    public InfrastructureInventoryServiceClient(InventoryServiceGrpc.InventoryServiceBlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReleaseReservedStock")
    public void releaseReservedStock(SharedOrderSnapshotDTO orderSnapshot) {
        try {
            ReleaseStockRequest request = convertToGrpcRequest(orderSnapshot);
            grpcStub.releaseReservedStock(request);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("InventoryService", ex.getMessage(), ex);
        }
    }

    @Override
    public void incrementStock(SharedReturnRequestDTO returnRequest) {
        try {
            IncrementStockRequest request = IncrementStockRequest.newBuilder()
                .addAllItems(returnRequest.getItems().stream()
                    .map(item -> StockItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .build())
                    .collect(Collectors.toList()))
                .build();
            grpcStub.incrementStock(request);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("InventoryService", ex.getMessage(), ex);
        }
    }

    @Override
    public boolean checkStockAvailability(String orderId) {
        try {
            CheckStockRequest request = CheckStockRequest.newBuilder()
                .setOrderId(orderId)
                .build();
            CheckStockResponse response = grpcStub.checkStockAvailability(request);
            return response.getAvailable();
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("InventoryService", ex.getMessage(), ex);
        }
    }

    private ReleaseStockRequest convertToGrpcRequest(SharedOrderSnapshotDTO orderSnapshot) {
        List<StockItem> items = orderSnapshot.getItems().stream()
            .map(item -> StockItem.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(item.getQuantity())
                .build())
            .collect(Collectors.toList());
        return ReleaseStockRequest.newBuilder()
            .addAllItems(items)
            .build();
    }

    @SuppressWarnings("unused")
    private void fallbackReleaseReservedStock(SharedOrderSnapshotDTO orderSnapshot, Throwable ex) {
        throw new InfrastructureExternalServiceException("InventoryService", "Fallback failed: " + ex.getMessage(), ex);
    }
}