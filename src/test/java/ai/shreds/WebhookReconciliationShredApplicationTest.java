package ai.shreds;

import ai.shreds.infrastructure.external_services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = WebhookReconciliationShredApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
public class WebhookReconciliationShredApplicationTest {

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
        
        // Set minimum required scheduling pool size (must be >= 1)
        registry.add("spring.task.scheduling.pool.size", () -> "1");
        
        // Disable the actual scheduler execution with very long delays
        registry.add("scheduling.correlation.fixed-delay", () -> "9999999");
        registry.add("scheduling.reconciliation.fixed-delay", () -> "9999999");
        
        // Test webhook secrets
        registry.add("webhook.stripe.signing-secret", () -> "whsec_test_secret_for_integration_tests");
        registry.add("webhook.paypal.cert-url", () -> "https://api.sandbox.paypal.com/v1/notifications/webhooks-events");
        registry.add("webhook.square.application-secret", () -> "sq_app_secret_test");
    }

    @Test
    public void contextLoads() {
        // This test will pass if the application context loads successfully
        assertThat(port).isGreaterThan(0);
        System.out.println("✅ Application started successfully on port: " + port);
        System.out.println("✅ PostgreSQL container running on: " + postgres.getJdbcUrl());
        System.out.println("✅ Kafka container running on: " + kafka.getBootstrapServers());
    }
}
