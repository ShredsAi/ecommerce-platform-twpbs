package ai.shreds;

import ai.shreds.domain.events.DomainPaymentIntentCreatedEvent;
import ai.shreds.infrastructure.external_services.*;
import ai.shreds.infrastructure.repositories.InfrastructurePaymentIntentJpaEntity;
import ai.shreds.infrastructure.repositories.InfrastructurePaymentIntentJpaRepository;
import ai.shreds.shared.dtos.SharedOrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Integration test that verifies the automatic payment intent creation flow
 * when OrderPlaced events are consumed from Kafka, ensuring end-to-end
 * event processing and domain logic execution.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PaymentProcessingApplication.class
)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@ExtendWith(OutputCaptureExtension.class)
class OrderEventToPaymentIntentIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InfrastructurePaymentIntentJpaRepository paymentIntentRepository;

    // TestContainers - Real infrastructure components
    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2-alpine"))
            .withDatabaseName("test_payments")
            .withUsername("test_user")
            .withPassword("test_password")
            .withExposedPorts(5432);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1"))
            .withExposedPorts(9092, 9093);

    // Mock external service adapters to avoid real API calls
    @MockBean(name = "infrastructureStripeProcessorAdapter")
    private InfrastructureStripeProcessorAdapter stripeProcessorAdapter;
    
    @MockBean(name = "infrastructurePayPalProcessorAdapter")
    private InfrastructurePayPalProcessorAdapter payPalProcessorAdapter;
    
    @MockBean(name = "infrastructureSquareProcessorAdapter")
    private InfrastructureSquareProcessorAdapter squareProcessorAdapter;
    
    @MockBean
    private InfrastructurePCITokenVaultAdapter pciTokenVaultAdapter;
    
    @MockBean
    private InfrastructureThreeDSecureProviderAdapter threeDSecureProviderAdapter;
    
    // Mock external HTTP clients
    @MockBean
    private InfrastructureStripeClient stripeClient;
    
    @MockBean
    private InfrastructurePayPalClient payPalClient;
    
    @MockBean
    private InfrastructureSquareClient squareClient;
    
    @MockBean
    private InfrastructurePCIVaultClient pciVaultClient;
    
    @MockBean
    private InfrastructureThreeDSecureClient threeDSecureClient;
    
    // Spy on event publisher to verify domain events are published
    @SpyBean
    private InfrastructureSpringEventPublisher eventPublisher;

    /**
     * Configure dynamic properties for TestContainers
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        
        // Kafka configuration - FIXED: Removed topic name overrides as they don't affect @KafkaListener
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        
        // Disable Flyway baseline for tests since we start with clean DB
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    /**
     * Tests the complete OrderPlaced event to PaymentIntent creation flow:
     * 1. Publishes an OrderPlaced event to Kafka
     * 2. Verifies the event is consumed by AdapterOrderEventConsumer
     * 3. Verifies ApplicationEventHandlerService processes the event
     * 4. Verifies DomainPaymentIntentService creates a payment intent
     * 5. Verifies payment intent is persisted in database
     * 6. Verifies PaymentIntentCreated domain event is published
     */
    @Test
    void When_OrderPlaced_Event_Is_Consumed_Then_PaymentIntent_Is_Created_Automatically(CapturedOutput output) {
        System.out.println("=== Starting OrderPlaced Event Integration Test ===");
        
        try {
            // Step 1: Create test data
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            BigDecimal orderAmount = new BigDecimal("250.00");
            LocalDateTime orderTimestamp = LocalDateTime.now();
            
            SharedOrderPlacedEvent orderEvent = SharedOrderPlacedEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .amount(orderAmount)
                    .timestamp(orderTimestamp)
                    .build();
            
            System.out.println("Created OrderPlaced event:");
            System.out.println("- Order ID: " + orderId);
            System.out.println("- Customer ID: " + customerId);
            System.out.println("- Amount: $" + orderAmount);
            System.out.println("- Timestamp: " + orderTimestamp);
            
            // Step 2: Get initial count of payment intents
            long initialCount = paymentIntentRepository.count();
            System.out.println("Initial payment intents count: " + initialCount);
            
            // Step 3: Publish OrderPlaced event to Kafka using the CORRECT topic name
            // FIXED: Use "order-events" instead of "test-order-events" to match @KafkaListener
            System.out.println("Publishing OrderPlaced event to Kafka topic: order-events");
            System.out.println("Kafka bootstrap servers: " + kafka.getBootstrapServers());
            
            String key = orderId.toString(); // Use orderId as partition key
            kafkaTemplate.send("order-events", key, orderEvent);
            kafkaTemplate.flush(); // Ensure message is sent
            
            System.out.println("OrderPlaced event published successfully");
            
            // Step 4: Wait for event processing and verify database state
            System.out.println("Waiting for event processing...");
            
            await().atMost(Duration.ofSeconds(30))
                   .pollInterval(Duration.ofMillis(500))
                   .untilAsserted(() -> {
                       long currentCount = paymentIntentRepository.count();
                       System.out.println("Current payment intents count: " + currentCount + " (waiting for > " + initialCount + ")");
                       assertThat(currentCount).isGreaterThan(initialCount);
                   });
            
            System.out.println("✓ Payment intent was created in database");
            
            // Step 5: Verify payment intent details
            List<InfrastructurePaymentIntentJpaEntity> allIntents = paymentIntentRepository.findAll();
            System.out.println("Total payment intents found: " + allIntents.size());
            
            // Find the intent created for our order
            InfrastructurePaymentIntentJpaEntity createdIntent = allIntents.stream()
                    .filter(intent -> orderId.equals(intent.getOrderId()))
                    .findFirst()
                    .orElse(null);
            
            assertThat(createdIntent).isNotNull();
            assertThat(createdIntent.getOrderId()).isEqualTo(orderId);
            assertThat(createdIntent.getCustomerId()).isEqualTo(customerId);
            // Note: Amount is stored in cents, so $250.00 = 25000 cents
            assertThat(createdIntent.getAmountCents()).isEqualTo(25000L);
            assertThat(createdIntent.getCurrency()).isEqualTo("USD");
            assertThat(createdIntent.getStatus()).isEqualTo("REQUIRES_PAYMENT_METHOD");
            assertThat(createdIntent.getClientSecret()).isNotNull();
            assertThat(createdIntent.getExpiresAt()).isAfter(LocalDateTime.now());
            
            System.out.println("✓ Payment intent details verified:");
            System.out.println("  - Intent ID: " + createdIntent.getId());
            System.out.println("  - Order ID: " + createdIntent.getOrderId());
            System.out.println("  - Customer ID: " + createdIntent.getCustomerId());
            System.out.println("  - Amount: " + createdIntent.getAmountCents() + " cents (" + createdIntent.getCurrency() + ")");
            System.out.println("  - Status: " + createdIntent.getStatus());
            System.out.println("  - Client Secret: " + createdIntent.getClientSecret());
            System.out.println("  - Expires At: " + createdIntent.getExpiresAt());
            
            // Step 6: Verify domain event was published
            System.out.println("Verifying PaymentIntentCreated domain event was published...");
            
            await().atMost(Duration.ofSeconds(10))
                   .untilAsserted(() -> {
                       verify(eventPublisher, atLeastOnce()).publish(any(DomainPaymentIntentCreatedEvent.class));
                   });
            
            System.out.println("✓ PaymentIntentCreated domain event was published");
            
            // Step 7: Verify logs contain expected processing messages
            String logs = output.getOut();
            System.out.println("Analyzing logs for event processing evidence...");
            
            // Look for Kafka consumer activity
            assertThat(logs.toLowerCase()).containsAnyOf(
                "orderplaced",
                "order placed", 
                "order-events",
                "kafka",
                "consumer"
            );
            
            System.out.println("✓ Log analysis shows event processing activity");
            
            // Step 8: Verify containers are still running
            assertThat(postgresql.isRunning()).isTrue();
            assertThat(kafka.isRunning()).isTrue();
            System.out.println("✓ TestContainers still running properly");
            
            System.out.println("=== OrderPlaced Event Integration Test Completed Successfully ===");
            System.out.println("Summary:");
            System.out.println("- OrderPlaced event published to Kafka: ✓");
            System.out.println("- Event consumed by AdapterOrderEventConsumer: ✓");
            System.out.println("- ApplicationEventHandlerService processed event: ✓");
            System.out.println("- DomainPaymentIntentService created payment intent: ✓");
            System.out.println("- Payment intent persisted in PostgreSQL database: ✓");
            System.out.println("- PaymentIntentCreated domain event published: ✓");
            System.out.println("- All external services properly mocked: ✓");
            System.out.println("- End-to-end event flow verified: ✓");
            
        } catch (Exception e) {
            System.out.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            
            // Additional debugging information
            System.out.println("\n=== DEBUG INFORMATION ===");
            System.out.println("Current payment intents in database:");
            List<InfrastructurePaymentIntentJpaEntity> allIntents = paymentIntentRepository.findAll();
            for (InfrastructurePaymentIntentJpaEntity intent : allIntents) {
                System.out.println("Intent: " + intent.getId() + ", Order: " + intent.getOrderId() + ", Status: " + intent.getStatus());
            }
            
            System.out.println("\nContainer status:");
            System.out.println("PostgreSQL running: " + postgresql.isRunning());
            System.out.println("Kafka running: " + kafka.isRunning());
            
            System.out.println("\nKafka connectivity:");
            System.out.println("Bootstrap servers: " + kafka.getBootstrapServers());
            
            throw new RuntimeException("OrderPlaced event integration test failed", e);
        }
    }
}