package ai.shreds.infrastructure.external_services;

import ai.shreds.application.dtos.ApplicationPricingRequestDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.application.ports.ApplicationPricingOutputPort;
import ai.shreds.domain.ports.DomainOutputPortPricingService;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import ai.shreds.shared.grpc.SharedGrpcCalculateOrderPricingRequest;
import ai.shreds.shared.grpc.SharedGrpcCalculateOrderPricingResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import ai.shreds.grpc.pricing.v1.PricingServiceGrpc;

@Component
public class InfrastructureGrpcPricingClient implements ApplicationPricingOutputPort, DomainOutputPortPricingService {

    private final PricingServiceGrpc.PricingServiceBlockingStub pricingStub;
    private final InfrastructureGrpcMapper grpcMapper;
    private final RetryTemplate retryTemplate;
    private final CircuitBreaker circuitBreaker;

    public InfrastructureGrpcPricingClient(PricingServiceGrpc.PricingServiceBlockingStub pricingStub,
                                            InfrastructureGrpcMapper grpcMapper,
                                            RetryTemplate retryTemplate,
                                            @Qualifier("pricingCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.pricingStub = pricingStub;
        this.grpcMapper = grpcMapper;
        this.retryTemplate = retryTemplate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public ApplicationPricingResponseDTO calculatePricing(ApplicationPricingRequestDTO request) {
        SharedGrpcCalculateOrderPricingRequest grpcReq = grpcMapper.toGrpcPricingRequest(request);
        SharedGrpcCalculateOrderPricingResponse grpcResp;
        try {
            grpcResp = retryTemplate.execute(ctx ->
                circuitBreaker.executeSupplier(() ->
                    SharedGrpcCalculateOrderPricingResponse.fromProto(
                        pricingStub.calculateOrderPricing(grpcReq.toProto())
                    )
                )
            );
        } catch (StatusRuntimeException e) {
            throw new InfrastructureExternalServiceException(
                "Pricing service call failed", e, "PricingService", e.getStatus().getCode().name(), grpcReq
            );
        } catch (Exception e) {
            throw new InfrastructureExternalServiceException(
                "Pricing service error after retries", e, "PricingService", "UNKNOWN", grpcReq
            );
        }
        return grpcMapper.fromGrpcPricingResponse(grpcResp);
    }
}