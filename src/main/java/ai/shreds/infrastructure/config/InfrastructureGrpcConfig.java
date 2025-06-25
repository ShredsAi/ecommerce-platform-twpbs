package ai.shreds.infrastructure.config;

import com.ordermanagement.order.OrderServiceGrpc;
import inventory.InventoryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for gRPC clients.
 */
@Configuration
public class InfrastructureGrpcConfig {

    @Value("${grpc.inventory.host}")
    private String inventoryHost;

    @Value("${grpc.inventory.port}")
    private Integer inventoryPort;

    @Value("${grpc.order.host}")
    private String orderHost;

    @Value("${grpc.order.port}")
    private Integer orderPort;

    @Bean
    public ManagedChannel inventoryServiceChannel() {
        return configureChannel(inventoryHost, inventoryPort);
    }

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub(ManagedChannel inventoryServiceChannel) {
        return InventoryServiceGrpc.newBlockingStub(inventoryServiceChannel);
    }

    @Bean
    public ManagedChannel orderServiceChannel() {
        return configureChannel(orderHost, orderPort);
    }

    @Bean
    public OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub(ManagedChannel orderServiceChannel) {
        return OrderServiceGrpc.newBlockingStub(orderServiceChannel);
    }

    private ManagedChannel configureChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(1024 * 1024 * 4) // 4MB
                .build();
    }
}