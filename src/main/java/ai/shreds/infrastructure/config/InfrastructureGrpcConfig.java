package ai.shreds.infrastructure.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ai.shreds.grpc.inventory.v1.InventoryServiceGrpc;
import ai.shreds.grpc.pricing.v1.PricingServiceGrpc;

@Configuration
public class InfrastructureGrpcConfig {

    @Value("${grpc.client.inventory.address}")
    private String inventoryServiceUrl;

    @Value("${grpc.client.pricing.address}")
    private String pricingServiceUrl;

    @Value("${grpc.client.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${grpc.client.timeout-seconds:5}")
    private int timeoutSeconds;

    @Bean(name = "inventoryServiceChannel")
    public ManagedChannel inventoryServiceChannel() {
        return ManagedChannelBuilder.forTarget(inventoryServiceUrl)
                .usePlaintext()
                .build();
    }

    @Bean(name = "pricingServiceChannel")
    public ManagedChannel pricingServiceChannel() {
        return ManagedChannelBuilder.forTarget(pricingServiceUrl)
                .usePlaintext()
                .build();
    }

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub(ManagedChannel inventoryServiceChannel) {
        return InventoryServiceGrpc.newBlockingStub(inventoryServiceChannel);
    }

    @Bean
    public PricingServiceGrpc.PricingServiceBlockingStub pricingServiceStub(ManagedChannel pricingServiceChannel) {
        return PricingServiceGrpc.newBlockingStub(pricingServiceChannel);
    }
}