package ai.shreds.infrastructure.external_services;

import ai.shreds.application.dtos.ApplicationInventoryCheckRequestDTO;
import ai.shreds.application.dtos.ApplicationInventoryCheckResponseDTO;
import ai.shreds.application.dtos.ApplicationInventoryItemDTO;
import ai.shreds.application.ports.ApplicationInventoryOutputPort;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import ai.shreds.shared.grpc.SharedGrpcValidateStockAvailabilityRequest;
import ai.shreds.shared.grpc.SharedGrpcValidateStockAvailabilityResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import ai.shreds.grpc.inventory.v1.InventoryServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * gRPC client for Inventory Service, with retry and circuit-breaker.
 */
@Component
public class InfrastructureGrpcInventoryClient implements ApplicationInventoryOutputPort, DomainOutputPortInventoryService {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
    private final InfrastructureGrpcMapper grpcMapper;
    private final RetryTemplate retryTemplate;
    private final CircuitBreaker circuitBreaker;

    public InfrastructureGrpcInventoryClient(InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub,
                                             InfrastructureGrpcMapper grpcMapper,
                                             RetryTemplate retryTemplate,
                                             @Qualifier("inventoryCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.inventoryStub = inventoryStub;
        this.grpcMapper = grpcMapper;
        this.retryTemplate = retryTemplate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public ApplicationInventoryCheckResponseDTO checkAvailability(ApplicationInventoryCheckRequestDTO request) {
        return verifyAvailability(request.getItems());
    }

    @Override
    public ApplicationInventoryCheckResponseDTO verifyAvailability(List<ApplicationInventoryItemDTO> items) {
        ApplicationInventoryCheckRequestDTO reqDto = new ApplicationInventoryCheckRequestDTO(items);
        SharedGrpcValidateStockAvailabilityRequest grpcReq = grpcMapper.toGrpcInventoryRequest(reqDto);
        SharedGrpcValidateStockAvailabilityResponse grpcResp;
        try {
            grpcResp = retryTemplate.execute(ctx -> 
                circuitBreaker.executeSupplier(() -> 
                    SharedGrpcValidateStockAvailabilityResponse.fromProto(
                        inventoryStub.validateStockAvailability(grpcReq.toProto())
                    )
                )
            );
        } catch (StatusRuntimeException e) {
            throw new InfrastructureExternalServiceException(
                "Inventory service call failed", e, "InventoryService", e.getStatus().getCode().name(), grpcReq
            );
        } catch (Exception e) {
            throw new InfrastructureExternalServiceException(
                "Inventory service error after retries", e, "InventoryService", "UNKNOWN", grpcReq
            );
        }
        return grpcMapper.fromGrpcInventoryResponse(grpcResp);
    }
}