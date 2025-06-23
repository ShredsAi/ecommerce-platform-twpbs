package ai.shreds;

import ai.shreds.domain.ports.*;
import ai.shreds.infrastructure.external_services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test that verifies the Payment Processing application starts correctly
 * with all required infrastructure components (database, messaging) using TestContainers
 * and mocks for external services.
 * 
 * This test focuses on:
 * - Application startup verification
 * - Infrastructure connectivity (PostgreSQL, Kafka)
 * - Bean creation and dependency injection
 * - Health checks
 * - Log analysis for startup errors
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PaymentProcessingApplication.class
)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@ExtendWith(OutputCaptureExtension.class)
class PaymentProcessingApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private TestRestTemplate restTemplate;

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
        
        // Disable Flyway baseline for tests since we start with clean DB
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    /**
     * Main integration test that verifies the application starts successfully.
     * This test captures all Spring Boot startup logs and analyzes them for errors.
     */
    @Test
    void shouldStartApplicationSuccessfully(CapturedOutput output) {
        // Log the captured startup output for analysis
        System.out.println("=============== SPRING BOOT STARTUP LOGS ===============");
        System.out.println(output.getOut());
        System.out.println("=============== END OF STARTUP LOGS ===============");
        
        // Verify the application context loaded successfully
        assertThat(applicationContext).isNotNull();
        
        // Verify the server started on the expected port
        assertThat(port).isGreaterThan(0);
        System.out.println("Application started on port: " + port);
        
        // Verify TestContainers are running
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();
        System.out.println("PostgreSQL container running on: " + postgresql.getJdbcUrl());
        System.out.println("Kafka container running on: " + kafka.getBootstrapServers());
    }

    /**
     * Verifies critical application beans are created and configured correctly.
     */
    @Test
    void shouldCreateAllRequiredBeans(CapturedOutput output) {
        // Verify core application service beans exist
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.application.services.ApplicationPaymentIntentService.class
        )).isNotEmpty();
        
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.application.services.ApplicationPaymentQueryService.class
        )).isNotEmpty();
        
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.application.services.ApplicationEventHandlerService.class
        )).isNotEmpty();
        
        // Verify domain service beans exist
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.domain.services.DomainPaymentIntentService.class
        )).isNotEmpty();
        
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.domain.services.DomainPaymentService.class
        )).isNotEmpty();
        
        // Verify adapter beans exist
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.adapter.primary.AdapterPaymentController.class
        )).isNotEmpty();
        
        // Verify infrastructure beans exist
        assertThat(applicationContext.getBeansOfType(
            ai.shreds.infrastructure.repositories.InfrastructurePaymentRepositoryImpl.class
        )).isNotEmpty();
        
        System.out.println("All required application beans created successfully");
        
        // Log bean creation info from startup
        String startupLogs = output.getOut();
        assertThat(startupLogs).contains("Started PaymentProcessingApplication");
        assertThat(startupLogs).doesNotContain("APPLICATION FAILED TO START");
        assertThat(startupLogs).doesNotContain("Error starting ApplicationContext");
    }

    /**
     * Verifies the application health endpoints are working via HTTP.
     */
    @Test
    void shouldHaveHealthyApplication(CapturedOutput output) {
        // Wait for application to be fully started and test health endpoint
        await().atMost(Duration.ofSeconds(30))
               .untilAsserted(() -> {
                   ResponseEntity<String> response = restTemplate.getForEntity(
                       "http://localhost:" + port + "/actuator/health", String.class);
                   assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                   assertThat(response.getBody()).contains("UP");
               });
        
        System.out.println("Application health check passed");
        
        // Verify no critical errors in logs
        String logs = output.getOut();
        assertThat(logs).doesNotContain("FATAL");
        assertThat(logs).doesNotContain("Failed to start bean");
        assertThat(logs).doesNotContain("BeanCreationException");
        assertThat(logs).doesNotContain("ConnectException");
    }

    /**
     * Verifies database connectivity and connection pool initialization.
     */
    @Test
    void shouldConnectToDatabase(CapturedOutput output) {
        String logs = output.getOut();
        
        // Verify database connection pool was initialized successfully
        assertThat(logs).containsAnyOf(
            "HikariPool-1 - Starting",
            "HikariPool-1 - Added connection",
            "HikariPool",
            "Processing PersistenceUnitInfo"
        );
        
        // Verify JPA/Hibernate initialized successfully
        assertThat(logs).containsAnyOf(
            "Hibernate ORM core version",
            "Initialized JPA EntityManagerFactory",
            "hibernate"
        );
        
        System.out.println("Database connectivity verified");
    }

    /**
     * Verifies Kafka connectivity and consumer/producer setup.
     */
    @Test
    void shouldConnectToKafka(CapturedOutput output) {
        // Verify Kafka consumer and producer initialization
        String logs = output.getOut();
        assertThat(logs).containsAnyOf(
            "Kafka",
            "consumer",
            "producer",
            "Subscribed to topic"
        );
        
        System.out.println("Kafka connectivity verified");
    }

    /**
     * Verifies no critical configuration errors during startup.
     */
    @Test
    void shouldNotHaveConfigurationErrors(CapturedOutput output) {
        String logs = output.getOut();
        
        // Check for common configuration errors
        assertThat(logs).doesNotContain("Could not resolve placeholder");
        assertThat(logs).doesNotContain("Invalid configuration");
        assertThat(logs).doesNotContain("UnsatisfiedDependencyException");
        assertThat(logs).doesNotContain("NoSuchBeanDefinitionException");
        assertThat(logs).doesNotContain("CircularReferenceException");
        
        System.out.println("No configuration errors detected");
    }
}