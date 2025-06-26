package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.ports.DomainOutputPortShippingService;
import ai.shreds.shared.dtos.SharedCreateShipmentRequestDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.dtos.ShipmentResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InfrastructureShippingServiceClient implements DomainOutputPortShippingService {

    private final InfrastructureShippingGrpcStub grpcStub;

    public InfrastructureShippingServiceClient(InfrastructureShippingGrpcStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    @Override
    public ShipmentResult createShipment(DomainOrderEntity order) {
        List<SharedOrderItemDTO> items = order.getOrderItems().stream()
                .map(item -> new SharedOrderItemDTO(
                        item.getOrderItemId(),
                        item.getProductId().getValue(),
                        item.getQuantity().getValue(),
                        item.getUnitPrice(),
                        item.getTotalPrice()))
                .collect(Collectors.toList());
        SharedCreateShipmentRequestDTO request = new SharedCreateShipmentRequestDTO(
                order.getOrderId().getValue().toString(),
                items,
                order.getShippingAddress(),
                false);
        return grpcStub.createShipment(request);
    }

    @Override
    public boolean cancelShipment(String trackingNumber) {
        return grpcStub.cancelShipment(trackingNumber);
    }

    @Override
    public void subscribeToUpdates(UUID orderId) {
        grpcStub.streamUpdates(orderId);
    }
}