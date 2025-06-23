package ai.shreds;

import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentStatusUpdateEntity;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.infrastructure.external_services.*;
import ai.shreds.infrastructure.repositories.InfrastructurePaymentStatusUpdateJpaRepository;
import ai.shreds.shared.dtos.SharedPaymentWebhookProcessedEvent;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.context.ApplicationEventPublisher;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;

/**
 * Integration test for webhook processing and payment status reconciliation.
 * 
 * This test verifies the complete webhook processing flow:
 * 1. Webhook event processing
 * 2. Payment status reconciliation (processing webhooks for already terminal payments)
 * 3. Status update record creation
 * 4. Kafka event publishing for success/failure
 * 5. End-to-end consistency between external processor state and internal payment state
 * 
 * Note: Payments in the database can only have terminal states (SUCCEEDED or FAILED) per schema constraints.
 * This test verifies webhook reconciliation where the status remains the same but is recorded for audit.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PaymentProcessingApplication.class
)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@ExtendWith(OutputCaptureExtension.class)
class WebhookProcessingAndReconciliationIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private DomainOutputPortPaymentRepository paymentRepository;
    
    @Autowired
    private InfrastructurePaymentStatusUpdateJpaRepository statusUpdateRepository;
    
    // Real Kafka publisher to verify event publishing
    @SpyBean
    private InfrastructureKafkaEventPublisher kafkaEventPublisher;
    
    // Mock external service adapters to avoid real API calls
    @MockBean
    private InfrastructureStripeProcessorAdapter stripeProcessorAdapter;
    
    @MockBean
    private InfrastructurePayPalProcessorAdapter payPalProcessorAdapter;
    
    @MockBean
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

    /**
     * Configure dynamic properties for TestContainers
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        
        // Kafka topic configuration
        registry.add("spring.kafka.topics.payment-events", () -> "test-payment-events");
        registry.add("kafka.topics.payment-events", () -> "test-payment-events");
        
        // Flyway configuration
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }
    
    // Test data
    private DomainPaymentIntentEntity testPaymentIntent;
    private DomainPaymentEntity testPayment;
    private UUID paymentId;
    private UUID paymentIntentId;
    private UUID failedPaymentId;
    private UUID failedIntentId;
    private UUID orderId;
    private UUID customerId;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        // Initialize base time to a past time to avoid timing issues
        baseTime = LocalDateTime.now().minusMinutes(10);
        
        // Initialize test IDs for successful payment
        paymentId = UUID.randomUUID();
        paymentIntentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        
        // Initialize test IDs for failed payment test
        failedPaymentId = UUID.randomUUID();
        failedIntentId = UUID.randomUUID();
        
        // Create test payment intent in SUCCEEDED status without payment_method_id to avoid FK constraint
        testPaymentIntent = createTestPaymentIntentForSuccessScenario();
        
        // Create test payment in SUCCEEDED status (per DB constraint, payments must be terminal)
        testPayment = createTestPaymentForSuccessScenario();
        
        // Save to database
        paymentRepository.savePaymentIntent(testPaymentIntent);
        paymentRepository.savePayment(testPayment);
        
        // Reset mocks
        reset(kafkaEventPublisher);
        
        System.out.println("Test setup complete - Payment ID: " + paymentId);
    }

    /**
     * Main integration test that validates webhook processing flow:
     * 1. Processes a payment webhook event
     * 2. Verifies webhook reconciliation (status confirmation without change)
     * 3. Verifies status update record creation
     * 4. Verifies Kafka event publishing based on reconciliation logic
     * 
     * Since payments can only be in terminal states (SUCCEEDED/FAILED) per DB constraints,
     * this test verifies webhook reconciliation where external processor confirms existing status.
     */
    @Test
    void When_PaymentWebhook_Is_Processed_Then_Payment_Status_Is_Updated_And_Events_Published(CapturedOutput output) {
        // Log test start
        System.out.println("=============== WEBHOOK PROCESSING INTEGRATION TEST START ===============");
        System.out.println("Testing webhook processing for payment ID: " + paymentId);
        
        // Test successful webhook reconciliation (SUCCEEDED webhook for SUCCEEDED payment)
        testSuccessfulWebhookReconciliation(output);
        
        // Test failed webhook reconciliation (FAILED webhook for FAILED payment)
        testFailedWebhookReconciliation(output);
        
        System.out.println("=============== WEBHOOK PROCESSING INTEGRATION TEST COMPLETE ===============");
    }
    
    private void testSuccessfulWebhookReconciliation(CapturedOutput output) {
        System.out.println("--- Testing SUCCESSFUL webhook reconciliation ---");
        
        // Verify initial state is SUCCEEDED
        DomainPaymentEntity initialPayment = paymentRepository.findPaymentById(
            new DomainPaymentIdValue(paymentId));
        assertThat(initialPayment).isNotNull();
        assertThat(initialPayment.getStatus()).isEqualTo(DomainPaymentStatusEnum.SUCCEEDED);
        System.out.println("✓ Initial payment state verified: " + initialPayment.getStatus());
        
        // Create successful webhook event (confirming existing SUCCEEDED status)
        SharedPaymentWebhookProcessedEvent successEvent = createSuccessfulWebhookEvent();
        
        // Give time for transaction to commit
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Publish webhook event via Spring Application Event
        eventPublisher.publishEvent(successEvent);
        
        // Wait for async processing to complete and verify payment status
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   // Verify payment status remains SUCCEEDED
                   DomainPaymentEntity updatedPayment = paymentRepository.findPaymentById(
                       new DomainPaymentIdValue(paymentId));
                   
                   assertThat(updatedPayment).isNotNull();
                   assertThat(updatedPayment.getStatus()).isEqualTo(DomainPaymentStatusEnum.SUCCEEDED);
                   
                   System.out.println("✓ Payment status confirmed as: " + updatedPayment.getStatus());
               });
        
        // Verify payment intent status remains SUCCEEDED
        DomainPaymentIntentEntity updatedIntent = paymentRepository.findPaymentIntentById(
            new DomainPaymentIntentIdValue(paymentIntentId));
        assertThat(updatedIntent).isNotNull();
        assertThat(updatedIntent.getStatus()).isEqualTo(DomainPaymentStatusEnum.SUCCEEDED);
        System.out.println("✓ Payment intent status confirmed as: " + updatedIntent.getStatus());
        
        // Wait for status update records to be created for reconciliation
        await().atMost(Duration.ofSeconds(10))
               .pollInterval(Duration.ofMillis(500))
               .untilAsserted(() -> {
                   List<DomainPaymentStatusUpdateEntity> statusUpdates = 
                       paymentRepository.findStatusUpdatesByPaymentId(new DomainPaymentIdValue(paymentId));
                   assertThat(statusUpdates).isNotEmpty();
                   System.out.println("✓ Status update record created for reconciliation, count: " + statusUpdates.size());
               });
        
        // Verify Kafka success event was published (webhook processor publishes based on status)
        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   verify(kafkaEventPublisher, atLeastOnce()).publishPaymentSucceeded(any());
                   System.out.println("✓ Kafka payment succeeded event published for reconciliation");
               });
        
        // Verify log output
        String logs = output.getOut();
        assertThat(logs).contains("Publishing payment succeeded event for payment ID: " + paymentId);
        System.out.println("✓ Success webhook reconciliation completed");
    }
    
    private void testFailedWebhookReconciliation(CapturedOutput output) {
        System.out.println("--- Testing FAILED webhook reconciliation ---");
        
        // Create new payment for failure test - already in FAILED state
        DomainPaymentIntentEntity failedIntent = createTestPaymentIntentWithId(failedIntentId, DomainPaymentStatusEnum.FAILED);
        DomainPaymentEntity failedPayment = createTestFailedPaymentWithId(failedPaymentId, failedIntentId);
        
        paymentRepository.savePaymentIntent(failedIntent);
        paymentRepository.savePayment(failedPayment);
        
        // Give time for transaction to commit
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Create failed webhook event (confirming existing FAILED status)
        SharedPaymentWebhookProcessedEvent failedEvent = createFailedWebhookEvent(failedPaymentId);
        
        // Reset mock to avoid interference
        reset(kafkaEventPublisher);
        
        // Publish webhook event
        eventPublisher.publishEvent(failedEvent);
        
        // Wait for async processing to complete
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   // Verify payment status remains FAILED
                   DomainPaymentEntity updatedPayment = paymentRepository.findPaymentById(
                       new DomainPaymentIdValue(failedPaymentId));
                   
                   assertThat(updatedPayment).isNotNull();
                   assertThat(updatedPayment.getStatus()).isEqualTo(DomainPaymentStatusEnum.FAILED);
                   
                   System.out.println("✓ Failed payment status confirmed as: " + updatedPayment.getStatus());
               });
        
        // Wait for status update records to be created for reconciliation
        await().atMost(Duration.ofSeconds(10))
               .pollInterval(Duration.ofMillis(500))
               .untilAsserted(() -> {
                   List<DomainPaymentStatusUpdateEntity> statusUpdates = 
                       paymentRepository.findStatusUpdatesByPaymentId(new DomainPaymentIdValue(failedPaymentId));
                   assertThat(statusUpdates).isNotEmpty();
                   System.out.println("✓ Status update record created for failed payment reconciliation, count: " + statusUpdates.size());
               });
        
        // Verify Kafka failure event was published
        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   verify(kafkaEventPublisher, atLeastOnce()).publishPaymentFailed(any());
                   System.out.println("✓ Kafka payment failed event published for reconciliation");
               });
        
        System.out.println("✓ Failed webhook reconciliation completed");
    }
    
    private SharedPaymentWebhookProcessedEvent createSuccessfulWebhookEvent() {
        Map<String, Object> processorResponse = new HashMap<>();
        processorResponse.put("transaction_id", "txn_123456789");
        processorResponse.put("status", "succeeded");
        processorResponse.put("processor_type", "stripe");
        processorResponse.put("auth_code", "AUTH123");
        
        return SharedPaymentWebhookProcessedEvent.builder()
                .paymentId(paymentId)
                .newStatus("SUCCEEDED")
                .processorResponse(processorResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private SharedPaymentWebhookProcessedEvent createFailedWebhookEvent(UUID failedPaymentId) {
        Map<String, Object> processorResponse = new HashMap<>();
        processorResponse.put("transaction_id", "txn_failed_123");
        processorResponse.put("status", "failed");
        processorResponse.put("processor_type", "stripe");
        processorResponse.put("failure_reason", "insufficient_funds");
        processorResponse.put("decline_code", "insufficient_funds");
        
        return SharedPaymentWebhookProcessedEvent.builder()
                .paymentId(failedPaymentId)
                .newStatus("FAILED")
                .processorResponse(processorResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private DomainPaymentIntentEntity createTestPaymentIntentForSuccessScenario() {
        // Create payment intent in SUCCEEDED status
        return createTestPaymentIntentWithId(paymentIntentId, DomainPaymentStatusEnum.SUCCEEDED);
    }
    
    private DomainPaymentIntentEntity createTestPaymentIntentWithId(UUID intentId, DomainPaymentStatusEnum status) {
        // Use null for payment_method_id to avoid foreign key constraint violation
        return DomainPaymentIntentEntity.reconstruct(
                new DomainPaymentIntentIdValue(intentId),
                new DomainOrderIdValue(orderId),
                new DomainCustomerIdValue(customerId),
                new DomainMoneyValue(new BigDecimal("100.00"), "USD"),
                status,
                null,  // No payment method to avoid FK constraint
                DomainPaymentProcessorTypeEnum.STRIPE,
                "pi_test_" + intentId + "_secret",
                baseTime.plusMinutes(30),
                baseTime,
                baseTime,
                1L
        );
    }
    
    private DomainPaymentEntity createTestPaymentForSuccessScenario() {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("processor_id", "pi_test_" + paymentId);
        successResponse.put("status", "succeeded");
        successResponse.put("auth_code", "AUTH123");
        
        // Create payment with SUCCEEDED status (per DB constraint)
        return DomainPaymentEntity.reconstruct(
                new DomainPaymentIdValue(paymentId),
                new DomainPaymentIntentIdValue(paymentIntentId),
                new DomainMoneyValue(new BigDecimal("100.00"), "USD"),
                DomainPaymentStatusEnum.SUCCEEDED,
                DomainPaymentProcessorTypeEnum.STRIPE,
                new DomainProcessorResponseValue(
                    "stripe", "succeeded", "Payment succeeded", successResponse.toString(), successResponse),
                baseTime.plusMinutes(1),
                baseTime,
                baseTime.plusMinutes(1),
                1L
        );
    }
    
    private DomainPaymentEntity createTestFailedPaymentWithId(UUID paymentId, UUID intentId) {
        Map<String, Object> failureResponse = new HashMap<>();
        failureResponse.put("processor_id", "pi_test_" + paymentId);
        failureResponse.put("status", "failed");
        failureResponse.put("failure_reason", "insufficient_funds");
        failureResponse.put("decline_code", "insufficient_funds");
        
        // Create payment with FAILED status (per DB constraint)
        return DomainPaymentEntity.reconstruct(
                new DomainPaymentIdValue(paymentId),
                new DomainPaymentIntentIdValue(intentId),
                new DomainMoneyValue(new BigDecimal("100.00"), "USD"),
                DomainPaymentStatusEnum.FAILED,
                DomainPaymentProcessorTypeEnum.STRIPE,
                new DomainProcessorResponseValue(
                    "stripe", "failed", "Payment failed: insufficient_funds", failureResponse.toString(), failureResponse),
                baseTime.plusMinutes(1),
                baseTime,
                baseTime.plusMinutes(1),
                1L
        );
    }
}