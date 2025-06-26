package ai.shreds.infrastructure.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class InfrastructureGrpcConfig {

    @Value("${grpc.inventory.host:localhost}")
    private String inventoryHost;

    @Value("${grpc.inventory.port:9090}")
    private Integer inventoryPort;

    @Value("${grpc.order.host:localhost}")
    private String orderHost;

    @Value("${grpc.order.port:9091}")
    private Integer orderPort;

    @Bean
    public ManagedChannel inventoryServiceChannel() {
        return configureChannel(inventoryHost, inventoryPort);
    }

    @Bean
    public ManagedChannel orderServiceChannel() {
        return configureChannel(orderHost, orderPort);
    }

    // Note: These stub beans would be available once .proto files are compiled
    // Commented out for now as the generated classes don't exist yet
    /*
    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub(@Qualifier("inventoryServiceChannel") ManagedChannel channel) {
        return InventoryServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(30, TimeUnit.SECONDS);
    }

    @Bean  
    public OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub(@Qualifier("orderServiceChannel") ManagedChannel channel) {
        return OrderServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(30, TimeUnit.SECONDS);
    }
    */

    private ManagedChannel configureChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // For development - use TLS in production
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(1024 * 1024 * 4) // 4MB
                .maxInboundMetadataSize(8192) // 8KB
                .idleTimeout(5, TimeUnit.MINUTES)
                .userAgent("order-cancellation-returns-service/1.0")
                .build();
    }
}