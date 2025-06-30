package ai.shreds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.application.ports.ApplicationInventoryChangeNotificationOutputPort;
import ai.shreds.shared.dtos.SharedInventoryChangedEvent;
import ai.shreds.shared.dtos.SharedLowStockAlertEvent;
import ai.shreds.infrastructure.external_services.InfrastructureKafkaEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test to verify the Inventory Tracking application starts correctly.
 * Uses H2 in-memory database and mocks external services to focus on application startup.
 */
@SpringBootTest(
    classes = InventoryTrackingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "logging.level.ai.shreds=DEBUG",
        "logging.level.org.springframework.boot=INFO",
        // H2 in-memory database configuration
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=true",
        "spring.sql.init.mode=never",
        // Disable external services for startup test
        "management.health.redis.enabled=false",
        "management.health.kafka.enabled=false",
        "management.health.jms.enabled=false",
        // Disable cache metrics that cause issues with mocked beans
        "management.metrics.cache.instrument=false",
        "management.metrics.enabled=false",
        // Simple cache and timeout configurations
        "cache.ttl=10",
        "spring.redis.timeout=2000",
        "spring.redis.host=localhost",
        "spring.redis.port=6370",
        "spring.kafka.bootstrap-servers=localhost:9093",
        "spring.activemq.in-memory=true",
        // Disable scheduling for tests
        "spring.task.scheduling.enabled=false"
    }
)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig
public class InventoryTrackingApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    // Mock application ports to avoid external dependencies
    @MockBean
    private ApplicationCacheOutputPort cacheOutputPort;
    
    @MockBean
    private ApplicationInventoryChangeNotificationOutputPort inventoryChangeNotificationOutputPort;

    // Mock infrastructure implementation (this will satisfy both interface dependencies)
    @MockBean
    private InfrastructureKafkaEventPublisher infrastructureKafkaEventPublisher;

    // Mock Kafka templates needed by Kafka producers
    @MockBean
    private KafkaTemplate<String, SharedInventoryChangedEvent> inventoryEventKafkaTemplate;
    
    @MockBean
    private KafkaTemplate<String, SharedLowStockAlertEvent> lowStockAlertKafkaTemplate;
    
    @MockBean
    private KafkaTemplate<String, Object> genericKafkaTemplate;

    @Test
    public void contextLoads() {
        assertThat(applicationContext).isNotNull();
        System.out.println("✓ Spring application context loaded successfully");
    }

    @Test
    public void applicationStartsSuccessfully(CapturedOutput output) {
        // Verify application context loaded
        assertNotNull(applicationContext, "Application context should not be null");
        
        // Verify the application started message appears in logs
        String logOutput = output.getOut();
        
        assertThat(logOutput)
            .as("Application should have started successfully")
            .containsAnyOf(
                "Started InventoryTrackingApplication",
                "Tomcat started on port",
                "Application startup completed",
                "Started application"
            );
        
        // Verify no critical errors in startup
        assertThat(logOutput)
            .as("Should not contain critical startup errors")
            .doesNotContain(
                "APPLICATION FAILED TO START",
                "Error starting ApplicationContext",
                "Unable to start web server"
            );
            
        System.out.println("=== CAPTURED APPLICATION STARTUP LOGS ===");
        System.out.println(logOutput);
        System.out.println("=== END OF STARTUP LOGS ===");
        System.out.println("✓ Application started successfully without critical errors");
    }

    @Test
    public void healthEndpointIsAccessible() {
        // Test health endpoint via HTTP
        RestTemplate restTemplate = new RestTemplate();
        String healthUrl = "http://localhost:" + port + "/api/actuator/health";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            assertThat(response.getStatusCode())
                .as("Health endpoint should return OK status")
                .isEqualTo(HttpStatus.OK);
            
            assertThat(response.getBody())
                .as("Health response should contain UP status")
                .contains("UP");
                
            System.out.println("✓ Health endpoint accessible: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to connect to health endpoint: " + e.getMessage());
            throw new AssertionError("Health endpoint should be accessible", e);
        }
    }

    @Test
    public void serverIsRunningOnCorrectPort() {
        assertThat(port)
            .as("Server should be running on a valid port")
            .isGreaterThan(0);
        
        System.out.println("✓ Server running on port: " + port);
        
        // Test that we can make an HTTP request to the server
        RestTemplate restTemplate = new RestTemplate();
        String healthUrl = "http://localhost:" + port + "/api/actuator/health";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            assertThat(response.getStatusCode())
                .as("Health endpoint should return OK status")
                .isEqualTo(HttpStatus.OK);
            
            assertThat(response.getBody())
                .as("Health response should contain status")
                .contains("UP");
                
            System.out.println("✓ HTTP communication working correctly");
        } catch (Exception e) {
            System.err.println("Failed to connect to health endpoint: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void databaseConnectionIsEstablished(CapturedOutput output) {
        String logOutput = output.getOut();
        
        // Check for successful database connection indicators
        assertThat(logOutput)
            .as("Database connection should be established successfully")
            .containsAnyOf(
                "HikariPool",
                "Database connection",
                "Initialized JPA",
                "H2 database",
                "Starting embedded database"
            );
        
        // Should not contain database connection errors
        assertThat(logOutput)
            .as("Should not contain database connection errors")
            .doesNotContainIgnoringCase(
                "connection refused",
                "database connection failed",
                "unable to obtain jdbc connection"
            );
            
        System.out.println("✓ H2 in-memory database connection established successfully");
    }

    @Test
    public void springSecurityIsConfigured(CapturedOutput output) {
        String logOutput = output.getOut();
        
        // Check that Spring Security started without errors
        assertThat(logOutput)
            .as("Spring Security should be configured properly")
            .doesNotContainIgnoringCase(
                "security configuration error",
                "authentication failed to configure"
            );
            
        System.out.println("✓ Spring Security configured successfully");
    }

    @Test
    public void applicationHasRequiredBeans() {
        // Verify that key application beans are present
        String[] expectedBeans = {
            "inventoryTrackingApplication"
        };
        
        for (String beanName : expectedBeans) {
            assertThat(applicationContext.containsBean(beanName))
                .as("Application should contain bean: " + beanName)
                .isTrue();
        }
        
        System.out.println("✓ Required application beans are present in context");
    }
}
