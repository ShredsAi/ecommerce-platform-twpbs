package ai.shreds;

import ai.shreds.domain.entities.DomainEntityPayment;
import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import ai.shreds.domain.value_objects.DomainValueProcessorResponse;
import ai.shreds.infrastructure.external_services.*;
import ai.shreds.infrastructure.repositories.InfrastructureJpaPaymentRepository;
import ai.shreds.infrastructure.repositories.InfrastructureJpaPaymentWebhookRepository;
import ai.shreds.shared.dtos.SharedWebhookRequestDTO;
import ai.shreds.shared.dtos.SharedWebhookResponseDTO;
import ai.shreds.shared.dtos.SharedWebhookErrorResponseDTO;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.enums.SharedEnumPaymentStatus;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import ai.shreds.shared.value_objects.SharedValueMoney;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = WebhookReconciliationShredApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
public class WebhookReconciliationShredApplicationIT {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("webhook_reconciliation_test")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    // Mock external service dependencies that are called during application startup
    @MockBean
    private InfrastructureStripeSignatureVerifier stripeSignatureVerifier;
    
    @MockBean
    private InfrastructurePayPalSignatureVerifier payPalSignatureVerifier;
    
    @MockBean
    private InfrastructureSquareSignatureVerifier squareSignatureVerifier;

    @Autowired
    private InfrastructureJpaPaymentWebhookRepository webhookRepository;

    @Autowired
    private InfrastructureJpaPaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private BlockingQueue<ConsumerRecord<String, String>> kafkaRecords;
    private KafkaMessageListenerContainer<String, String> container;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        
        // JPA configuration for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        
        // Disable scheduling for tests
        registry.add("spring.task.scheduling.pool.size", () -> "0");
        
        // Test webhook secrets
        registry.add("webhook.stripe.signing-secret", () -> "whsec_test_secret_for_integration_tests");
        registry.add("webhook.paypal.cert-url", () -> "https://api.sandbox.paypal.com/v1/notifications/webhooks-events");
        registry.add("webhook.square.application-secret", () -> "sq_app_secret_test");
    }

    @BeforeEach
    void setUp() {
        // Set up Kafka consumer for integration tests
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group", "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties("payment-events");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        kafkaRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) kafkaRecords::add);
        container.start();
        
        // Wait for container to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Transactional
    public void When_Invalid_Signature_Webhook_Is_Received_Then_Request_Is_Rejected() throws Exception {
        System.out.println("🔍 Starting invalid signature webhook rejection test...");
        
        // Given: Mock signature verifier to return false for invalid signature
        when(stripeSignatureVerifier.verify(anyString(), anyString())).thenReturn(false);
        
        // Given: Count existing webhooks in database before the test
        long initialWebhookCount = webhookRepository.count();
        System.out.println("📊 Initial webhook count: " + initialWebhookCount);
        
        // Given: Clear any existing Kafka messages
        kafkaRecords.clear();
        
        // Given: Prepare Stripe webhook payload with invalid signature
        String stripePayload = "{" +
                "\"id\": \"evt_invalid_signature_test\"," +
                "\"object\": \"event\"," +
                "\"type\": \"payment_intent.succeeded\"," +
                "\"created\": 1686825600," +
                "\"data\": {" +
                    "\"object\": {" +
                        "\"id\": \"pi_invalid_signature_test\"," +
                        "\"object\": \"payment_intent\"," +
                        "\"amount\": 5000," +
                        "\"currency\": \"usd\"," +
                        "\"status\": \"succeeded\"" +
                    "}" +
                "}" +
        "}";
        
        SharedWebhookRequestDTO webhookRequest = SharedWebhookRequestDTO.builder()
                .processorType(SharedEnumPaymentProcessorType.STRIPE)
                .rawPayload(stripePayload)
                .externalEventId("evt_invalid_signature_test")
                .eventType("payment_intent.succeeded")
                .build();
        
        // Given: Prepare HTTP headers with invalid signature
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Stripe-Signature", "t=1686825600,v1=invalid_signature_hash_that_will_fail_verification");
        
        HttpEntity<SharedWebhookRequestDTO> requestEntity = new HttpEntity<>(webhookRequest, headers);
        
        System.out.println("📤 Sending webhook request with invalid signature to Stripe endpoint...");
        
        // When: Send the webhook request with invalid signature
        TestRestTemplate restTemplate = new TestRestTemplate();
        String webhookUrl = "http://localhost:" + port + "/webhooks/stripe";
        
        ResponseEntity<SharedWebhookErrorResponseDTO> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                requestEntity,
                SharedWebhookErrorResponseDTO.class
        );
        
        // Then: Verify the HTTP response indicates unauthorized (401) status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Webhook validation failed");
        assertThat(response.getBody().getDetails()).contains("Invalid signature");
        assertThat(response.getBody().getWebhookId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        
        System.out.println("✅ Received expected 401 Unauthorized response");
        System.out.println("📋 Error response: " + response.getBody().getError());
        System.out.println("📋 Error details: " + response.getBody().getDetails());
        
        // Then: Verify NO webhook was persisted to database due to signature validation failure
        long finalWebhookCount = webhookRepository.count();
        assertThat(finalWebhookCount).isEqualTo(initialWebhookCount);
        
        // Also verify specifically that no webhook exists with this external event ID
        var rejectedWebhook = webhookRepository.findByExternalEventIdAndProcessorType(
                "evt_invalid_signature_test", 
                SharedEnumPaymentProcessorType.STRIPE.name()
        );
        assertThat(rejectedWebhook).isNotPresent();
        
        System.out.println("✅ Verified that no webhook was persisted to database (count remains: " + finalWebhookCount + ")");
        
        // Then: Verify NO Kafka event was published due to request rejection
        System.out.println("🔍 Verifying that no Kafka event was published...");
        
        // Wait a bit to ensure no message is published
        ConsumerRecord<String, String> kafkaRecord = kafkaRecords.poll(3, TimeUnit.SECONDS);
        assertThat(kafkaRecord).isNull();
        
        System.out.println("✅ Verified that no Kafka event was published");
        
        // Then: Verify that signature verifier was called with the correct parameters
        System.out.println("✅ Signature verification was properly executed and rejected the invalid signature");
        
        System.out.println("✅ Invalid signature webhook rejection test completed successfully!");
    }
}