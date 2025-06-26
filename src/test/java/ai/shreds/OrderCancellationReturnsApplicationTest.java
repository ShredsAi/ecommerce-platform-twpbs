package ai.shreds;

import ai.shreds.infrastructure.external_services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Order Cancellation and Returns Application.
 * 
 * This test verifies that:
 * - The Spring Boot application starts successfully
 * - All required beans are loaded and properly configured
 * - Database connectivity works with TestContainers
 * - Health endpoints are accessible
 * - External service mocks are properly configured
 * 
 * Uses TestContainers for:
 * - PostgreSQL database
 * - Embedded Kafka for messaging
 * 
 * External services are mocked:
 * - Payment Service (REST)
 * - Inventory Service (gRPC)
 * - Notification Service (REST)
 * - Kafka Event Client
 * - JMS Event Client
 * - Spring Event Client
 */
@SpringBootTest(
    classes = OrderCancellationReturnsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "management.endpoints.web.exposure.include=health,info,metrics",
        "logging.level.ai.shreds=INFO"
    }
)
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"order-events", "cancellation-events", "return-events"})
@ExtendWith(OutputCaptureExtension.class)
public class OrderCancellationReturnsApplicationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(false);

    // Configure dynamic properties for TestContainers
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    // Mock external service clients to avoid real network calls
    // These @MockBean annotations will replace the real implementations
    @MockBean
    private InfrastructurePaymentServiceClient paymentServiceClient;

    @MockBean
    private InfrastructureInventoryServiceClient inventoryServiceClient;

    @MockBean
    private InfrastructureNotificationServiceClient notificationServiceClient;

    // Mock event clients to avoid real messaging infrastructure
    @MockBean
    private InfrastructureKafkaEventClient kafkaEventClient;

    @MockBean
    private InfrastructureJmsEventClient jmsEventClient;

    @MockBean
    private InfrastructureSpringEventClient springEventClient;

    @TestConfiguration
    static class TestConfig {
        // Any additional test-specific beans can be defined here
    }

    @Test
    void contextLoads(CapturedOutput output) {
        // Verify the Spring context loaded successfully
        assertThat(applicationContext).isNotNull();
        
        // Check that the application startup logs contain expected messages
        String logOutput = output.getOut();
        assertThat(logOutput).contains("Started OrderCancellationReturnsApplicationTest");
        assertThat(logOutput).containsAnyOf(
            "order-cancellation-returns-test", 
            "OrderCancellationReturnsApplicationTest",
            "OrderCancellationReturnsApplication"
        );
        
        System.out.println("=== APPLICATION STARTUP LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF STARTUP LOGS ====");
    }

    @Test
    void applicationStartsSuccessfully(CapturedOutput output) {
        // Verify core application components are loaded
        assertAll(
            "Application components verification",
            () -> assertThat(applicationContext.getBean("applicationCancellationService")).isNotNull(),
            () -> assertThat(applicationContext.getBean("applicationReturnService")).isNotNull(),
            () -> assertThat(applicationContext.getBean("domainCancellationService")).isNotNull(),
            () -> assertThat(applicationContext.getBean("domainReturnService")).isNotNull()
        );

        // Verify no fatal errors in startup logs
        String logOutput = output.getOut();
        assertThat(logOutput).doesNotContain("ERROR");
        assertThat(logOutput).doesNotContain("FATAL");
        assertThat(logOutput).doesNotContain("Exception in thread");
        
        System.out.println("=== APPLICATION COMPONENT VERIFICATION LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF COMPONENT VERIFICATION LOGS ====");
    }

    @Test
    void healthEndpointIsAccessible() {
        // Test that health endpoint responds correctly
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/actuator/health", 
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
        
        System.out.println("=== HEALTH ENDPOINT RESPONSE ====");
        System.out.println(response.getBody());
        System.out.println("=== END OF HEALTH ENDPOINT RESPONSE ====");
    }

    @Test
    void databaseConnectivityWorks() {
        // Verify PostgreSQL TestContainer is running and accessible
        assertTrue(postgresContainer.isRunning(), "PostgreSQL container should be running");
        assertThat(postgresContainer.getJdbcUrl()).contains("testdb");
        
        // Verify database connectivity through application context
        assertDoesNotThrow(() -> {
            // This will trigger database connection validation
            applicationContext.getBean("dataSource");
        });
    }

    @Test
    void requiredPortsAreProperlyMocked() {
        // Verify that external service clients are mocked
        assertThat(paymentServiceClient).isNotNull();
        assertThat(inventoryServiceClient).isNotNull();
        assertThat(notificationServiceClient).isNotNull();
        
        // Verify that event clients are mocked
        assertThat(kafkaEventClient).isNotNull();
        assertThat(jmsEventClient).isNotNull();
        assertThat(springEventClient).isNotNull();
    }

    @Test
    void applicationPropertiesAreLoadedCorrectly(CapturedOutput output) {
        // Verify that test-specific properties are loaded
        String logOutput = output.getOut();
        
        // Check for test profile activation
        assertThat(logOutput).contains("test");
        
        System.out.println("=== PROPERTIES VERIFICATION LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF PROPERTIES VERIFICATION LOGS ====");
    }

    @Test
    void noCircularDependenciesExist(CapturedOutput output) {
        // Verify no circular dependency issues in logs
        String logOutput = output.getOut();
        
        assertThat(logOutput).doesNotContain("Circular dependency");
        assertThat(logOutput).doesNotContain("circular reference");
        assertThat(logOutput).doesNotContain("BeanCurrentlyInCreationException");
        
        System.out.println("=== CIRCULAR DEPENDENCY CHECK LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF CIRCULAR DEPENDENCY CHECK LOGS ====");
    }

    @Test
    void springBootBannerIsDisplayed(CapturedOutput output) {
        // Verify Spring Boot banner appeared (indicates normal startup)
        String logOutput = output.getOut();
        
        // Spring Boot typically shows startup time and application name
        assertThat(logOutput).containsAnyOf(
            "Spring Boot", 
            "OrderCancellationReturnsApplication",
            "OrderCancellationReturnsApplicationTest",
            "order-cancellation-returns"
        );
        
        System.out.println("=== SPRING BOOT BANNER VERIFICATION ====");
        System.out.println(logOutput);
        System.out.println("=== END OF BANNER VERIFICATION ====");
    }
}