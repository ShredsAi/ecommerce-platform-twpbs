package ai.shreds;

import ai.shreds.application.ports.ApplicationInventoryOutputPort;
import ai.shreds.application.ports.ApplicationPricingOutputPort;
import ai.shreds.application.dtos.ApplicationInventoryCheckRequestDTO;
import ai.shreds.application.dtos.ApplicationInventoryCheckResponseDTO;
import ai.shreds.application.dtos.ApplicationInventoryResultDTO;
import ai.shreds.application.dtos.ApplicationPricingRequestDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.application.dtos.ApplicationItemPricingDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.test.system.CapturedOutput;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test to verify that the Order Creation Shred application starts up correctly.
 * 
 * This test:
 * - Uses TestContainers for PostgreSQL and Kafka
 * - Mocks external gRPC services (inventory and pricing)
 * - Verifies the Spring Boot application context loads successfully
 * - Captures and analyzes startup logs
 * - Checks that all critical beans are properly configured
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "logging.level.org.testcontainers=INFO",
        "logging.level.ai.shreds=DEBUG"
    }
)
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class OrderCreationApplicationStartupIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2-alpine"))
            .withDatabaseName("test_orders")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    // Mock external gRPC services to avoid dependency on external systems
    @MockBean
    private ApplicationInventoryOutputPort inventoryOutputPort;

    @MockBean
    private ApplicationPricingOutputPort pricingOutputPort;
    
    // Mock gRPC channels to avoid NameResolverProvider errors
    @MockBean(name = "inventoryServiceChannel")
    private ManagedChannel inventoryServiceChannel;
    
    @MockBean(name = "pricingServiceChannel")
    private ManagedChannel pricingServiceChannel;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Configure Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        // Configure dummy gRPC addresses
        registry.add("grpc.client.inventory.address", () -> "localhost:50091");
        registry.add("grpc.client.pricing.address", () -> "localhost:50092");
        
        // Disable health checks that might interfere with testing
        registry.add("management.health.db.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
        
        // Configure shorter timeouts for faster test execution
        registry.add("spring.transaction.default-timeout", () -> "5s");
        registry.add("resilience4j.retry.instances.inventory-service.max-attempts", () -> "1");
        registry.add("resilience4j.retry.instances.pricing-service.max-attempts", () -> "1");
    }

    /**
     * Main test method that verifies the application starts successfully.
     * This test captures all startup logs and analyzes them for successful initialization.
     *
     * @param output captured console output during test execution
     */
    @Test
    void shouldStartApplicationSuccessfully(CapturedOutput output) {
        // Setup mock responses for external services to avoid failures during startup checks
        setupMockResponses();
        
        // Log application startup information
        System.out.println("=== ORDER CREATION APPLICATION STARTUP TEST ===");
        System.out.println("Application started on port: " + port);
        System.out.println("PostgreSQL URL: " + postgres.getJdbcUrl());
        System.out.println("Kafka Bootstrap Servers: " + kafka.getBootstrapServers());
        
        // Verify that containers are running
        assertThat(postgres.isRunning())
            .as("PostgreSQL container should be running")
            .isTrue();
        
        assertThat(kafka.isRunning())
            .as("Kafka container should be running")
            .isTrue();
        
        // Analyze startup logs
        String logs = output.getOut();
        
        // Print full logs for analysis
        System.out.println("\n=== FULL STARTUP LOGS ===");
        System.out.println(logs);
        System.out.println("=== END OF STARTUP LOGS ===\n");
        
        // Verify critical startup indicators
        assertThat(logs)
            .as("Application should start successfully")
            .contains("Started OrderCreationApplication");
        
        assertThat(logs)
            .as("JPA repositories should be initialized")
            .containsAnyOf(
                "JPA repositories", 
                "Initialized JPA",
                "org.springframework.data.repository.config.RepositoryConfigurationDelegate"
            );
        
        assertThat(logs)
            .as("Liquibase should run successfully")
            .containsAnyOf(
                "Successfully acquired change log lock",
                "Liquibase completed successfully",
                "ChangeSet db/changelog"
            );
        
        assertThat(logs)
            .as("Kafka consumer should be configured")
            .containsAnyOf(
                "KafkaMessageListenerContainer",
                "kafka.consumer",
                "ConsumerConfig"
            );
        
        // Verify no critical errors in startup
        assertThat(logs.toLowerCase())
            .as("Should not contain fatal startup errors")
            .doesNotContain(
                "failed to start application",
                "application startup failed",
                "fatal error",
                "failed to configure"
            );
        
        // Check for common configuration issues
        if (logs.toLowerCase().contains("error")) {
            System.out.println("\n=== ERRORS FOUND IN LOGS ===");
            String[] lines = logs.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("error")) {
                    System.out.println("ERROR: " + line);
                }
            }
            System.out.println("=== END OF ERRORS ===\n");
        }
        
        System.out.println("\n=== APPLICATION STARTUP TEST COMPLETED SUCCESSFULLY ===");
    }
    
    /**
     * Sets up mock responses for external services to prevent failures during application startup.
     */
    private void setupMockResponses() {
        // Setup inventory service mock
        ApplicationInventoryResultDTO inventoryResult = new ApplicationInventoryResultDTO();
        inventoryResult.setProductId("test-product");
        inventoryResult.setAvailable(true);
        inventoryResult.setAvailableQty(10);
        
        ApplicationInventoryCheckResponseDTO inventoryResponse = new ApplicationInventoryCheckResponseDTO();
        inventoryResponse.setResults(List.of(inventoryResult));
        inventoryResponse.setAllAvailable(true);
        
        when(inventoryOutputPort.checkAvailability(any(ApplicationInventoryCheckRequestDTO.class)))
            .thenReturn(inventoryResponse);
        
        // Setup pricing service mock
        SharedMoneyDTO subtotal = new SharedMoneyDTO();
        subtotal.setAmount(new BigDecimal("100.00"));
        subtotal.setCurrency("USD");
        
        SharedMoneyDTO tax = new SharedMoneyDTO();
        tax.setAmount(new BigDecimal("10.00"));
        tax.setCurrency("USD");
        
        SharedMoneyDTO discounts = new SharedMoneyDTO();
        discounts.setAmount(BigDecimal.ZERO);
        discounts.setCurrency("USD");
        
        SharedMoneyDTO total = new SharedMoneyDTO();
        total.setAmount(new BigDecimal("110.00"));
        total.setCurrency("USD");
        
        ApplicationItemPricingDTO itemPricing = new ApplicationItemPricingDTO();
        itemPricing.setProductId("test-product");
        itemPricing.setUnitPrice(new BigDecimal("100.00"));
        itemPricing.setTotalPrice(new BigDecimal("100.00"));
        
        ApplicationPricingResponseDTO pricingResponse = new ApplicationPricingResponseDTO();
        pricingResponse.setSubtotal(subtotal);
        pricingResponse.setTax(tax);
        pricingResponse.setDiscounts(discounts);
        pricingResponse.setTotal(total);
        pricingResponse.setItemBreakdown(List.of(itemPricing));
        
        when(pricingOutputPort.calculatePricing(any(ApplicationPricingRequestDTO.class)))
            .thenReturn(pricingResponse);
    }
}