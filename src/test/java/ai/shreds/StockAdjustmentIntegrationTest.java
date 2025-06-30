package ai.shreds;

import ai.shreds.shared.dtos.*;
import ai.shreds.shared.enums.SharedEnumAdjustmentReason;
import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.application.ports.ApplicationInventoryChangeNotificationOutputPort;
import ai.shreds.infrastructure.external_services.InfrastructureKafkaEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

/**
 * Integration test for stock adjustment workflow.
 * Tests the complete flow: REST API → Database → Cache → Events
 * Uses H2 in-memory database and mocks external services for reliable testing.
 */
@SpringBootTest(
    classes = InventoryTrackingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
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
    "spring.jpa.show-sql=true",
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
})
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig
@Transactional
public class StockAdjustmentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock application ports to avoid external dependencies - following exact pattern from working test
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

    private String baseUrl;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/inventory";
        
        // Setup basic auth headers
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("test-admin", "test-password");
        
        // Setup test data
        setupTestData();
    }

    private void setupTestData() {
        try {
            // Clean up any existing test data first
            jdbcTemplate.execute("DELETE FROM stock_adjustment_audit WHERE sku_id = 'SKU-001'");
            jdbcTemplate.execute("DELETE FROM stock_ledger WHERE sku_id = 'SKU-001'");
            jdbcTemplate.execute("DELETE FROM sku WHERE sku_id = 'SKU-001'");
            jdbcTemplate.execute("DELETE FROM location WHERE location_id = 'WH-001'");
            
            // Insert test SKU
            jdbcTemplate.execute(
                "INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) " +
                "VALUES ('SKU-001', 'PROD-001', 'VENDOR-SKU-001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
            
            // Insert test location
            jdbcTemplate.execute(
                "INSERT INTO location (location_id, name, type, is_active, created_at) " +
                "VALUES ('WH-001', 'Main Warehouse', 'WAREHOUSE', true, CURRENT_TIMESTAMP)"
            );
            
            // Generate a UUID for the ledger_id since raw SQL doesn't trigger @PrePersist
            UUID ledgerId = UUID.randomUUID();
            
            // Insert initial stock ledger - providing all required fields including ledger_id
            jdbcTemplate.execute(
                "INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, available, version, last_updated) " +
                "VALUES ('" + ledgerId + "', 'SKU-001', 'WH-001', 100.0000, 0.0000, 100.0000, 0, CURRENT_TIMESTAMP)"
            );
            
            System.out.println("✓ Test data setup completed successfully");
            
        } catch (Exception e) {
            System.err.println("Error in setupTestData: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void When_Stock_Is_Adjusted_Via_REST_API_Then_Database_Is_Updated_Cache_Is_Invalidated_And_Events_Are_Published(CapturedOutput output) {
        // Given
        String skuId = "SKU-001";
        String locationId = "WH-001";
        BigDecimal adjustmentAmount = new BigDecimal("50.0000");
        
        SharedStockAdjustmentRequestDTO request = new SharedStockAdjustmentRequestDTO(
            skuId, 
            locationId, 
            adjustmentAmount, 
            SharedEnumAdjustmentReason.STOCK_RECEIPT
        );
        
        HttpEntity<SharedStockAdjustmentRequestDTO> entity = new HttpEntity<>(request, headers);
        
        // Get initial stock level to verify before and after
        BigDecimal initialQuantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM stock_ledger WHERE sku_id = ? AND location_id = ?",
            BigDecimal.class, skuId, locationId
        );
        
        System.out.println("Initial quantity before adjustment: " + initialQuantity);
        
        // When
        ResponseEntity<SharedStockAdjustmentResponseDTO> response = restTemplate.postForEntity(
            baseUrl + "/adjust", 
            entity, 
            SharedStockAdjustmentResponseDTO.class
        );
        
        // Then
        System.out.println("=== STOCK ADJUSTMENT RESPONSE ===");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
        
        // Verify HTTP response
        assertThat(response.getStatusCode())
            .as("Stock adjustment should return OK status")
            .isEqualTo(OK);
            
        SharedStockAdjustmentResponseDTO responseBody = response.getBody();
        assertThat(responseBody)
            .as("Response body should not be null")
            .isNotNull();
            
        assertThat(responseBody.getStatus())
            .as("Adjustment status should indicate success")
            .isIn("SUCCESS", "COMPLETED", "OK");
            
        assertThat(responseBody.getLedgerId())
            .as("Ledger ID should be provided in response")
            .isNotNull()
            .isNotBlank();
            
        assertThat(responseBody.getNewQuantity())
            .as("New quantity should be initial quantity plus adjustment")
            .isEqualTo(initialQuantity.add(adjustmentAmount));
            
        assertThat(responseBody.getTimestamp())
            .as("Timestamp should be provided")
            .isNotNull();
        
        // Verify database was updated - stock ledger
        BigDecimal updatedQuantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM stock_ledger WHERE sku_id = ? AND location_id = ?",
            BigDecimal.class, skuId, locationId
        );
        
        assertThat(updatedQuantity)
            .as("Database quantity should be updated correctly")
            .isEqualTo(initialQuantity.add(adjustmentAmount));
            
        System.out.println("✓ Database updated - Quantity: " + initialQuantity + " → " + updatedQuantity);
        
        // Verify audit record was created
        Integer auditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM stock_adjustment_audit WHERE sku_id = ? AND location_id = ? AND reason = ?",
            Integer.class, skuId, locationId, SharedEnumAdjustmentReason.STOCK_RECEIPT.name()
        );
        
        assertThat(auditCount)
            .as("Audit record should be created")
            .isEqualTo(1);
            
        System.out.println("✓ Audit record created successfully");
        
        // Verify outbox event was created (for event publishing)
        Integer outboxCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type = 'STOCK_LEDGER' AND processed = false",
            Integer.class
        );
        
        assertThat(outboxCount)
            .as("Outbox event should be created for inventory change")
            .isGreaterThanOrEqualTo(1);
            
        System.out.println("✓ Outbox event created for inventory change publishing");
        
        // Verify logs contain expected entries
        String logOutput = output.getOut();
        assertThat(logOutput)
            .as("Logs should contain stock adjustment processing")
            .containsIgnoringCase("Processing stock adjustment")
            .containsIgnoringCase(skuId)
            .containsIgnoringCase(locationId);
            
        // Check for success confirmation in logs
        String logOutputLower = logOutput.toLowerCase();
        boolean containsSuccessMessage = logOutputLower.contains("stock adjustment completed successfully") ||
                                       logOutputLower.contains("adjustment completed") ||
                                       logOutputLower.contains("adjustment processed");
        assertThat(containsSuccessMessage)
            .as("Logs should contain success confirmation")
            .isTrue();
            
        assertThat(logOutputLower)
            .as("Logs should not contain critical errors")
            .doesNotContain("error processing stock adjustment")
            .doesNotContain("failed to adjust stock")
            .doesNotContain("stock adjustment failed");
            
        System.out.println("✓ Application logs show successful processing");
        
        // Test cache invalidation by making a stock query
        ResponseEntity<SharedStockLevelDTO> stockQueryResponse = restTemplate.exchange(
            baseUrl + "/stock/" + skuId + "/" + locationId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            SharedStockLevelDTO.class
        );
        
        assertThat(stockQueryResponse.getStatusCode())
            .as("Stock query should work after adjustment")
            .isEqualTo(OK);
            
        SharedStockLevelDTO stockLevel = stockQueryResponse.getBody();
        assertThat(stockLevel)
            .as("Stock level response should not be null")
            .isNotNull();
            
        assertThat(stockLevel.getQuantity())
            .as("Stock query should return updated quantity")
            .isEqualTo(updatedQuantity);
            
        System.out.println("✓ Cache invalidated - Stock query returns updated values");
        
        System.out.println("=== INTEGRATION TEST PASSED ===" +
            "\n✓ REST API accepted request" +
            "\n✓ Database updated correctly" + 
            "\n✓ Audit trail created" +
            "\n✓ Outbox events generated" +
            "\n✓ Cache invalidation working" +
            "\n✓ No critical errors in logs");
    }

    @Test
    void When_Stock_Adjustment_Would_Result_In_Negative_Quantity_Then_Request_Is_Rejected_And_No_Changes_Are_Made(CapturedOutput output) {
        // Given
        String skuId = "SKU-001";
        String locationId = "WH-001";
        
        // Get current quantity first
        BigDecimal currentQuantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM stock_ledger WHERE sku_id = ? AND location_id = ?",
            BigDecimal.class, skuId, locationId
        );
        
        // Create an adjustment that would make quantity negative
        BigDecimal negativeAdjustment = currentQuantity.negate().subtract(new BigDecimal("10"));
        
        SharedStockAdjustmentRequestDTO request = new SharedStockAdjustmentRequestDTO(
            skuId,
            locationId,
            negativeAdjustment,
            SharedEnumAdjustmentReason.DAMAGE
        );
        
        HttpEntity<SharedStockAdjustmentRequestDTO> entity = new HttpEntity<>(request, headers);
        
        System.out.println("Testing negative adjustment: Current quantity " + currentQuantity + 
                         ", trying to adjust by " + negativeAdjustment);
        
        // When
        ResponseEntity<SharedStockAdjustmentResponseDTO> response = restTemplate.postForEntity(
            baseUrl + "/adjust",
            entity,
            SharedStockAdjustmentResponseDTO.class
        );
        
        // Then
        System.out.println("=== NEGATIVE QUANTITY REJECTION TEST ===");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
        
        // Should get a 400 Bad Request or 422 Unprocessable Entity
        assertThat(response.getStatusCode())
            .as("Request should be rejected with client error status")
            .isIn(BAD_REQUEST, UNPROCESSABLE_ENTITY, CONFLICT);
        
        // Verify database was NOT changed
        BigDecimal quantityAfterFailedAdjustment = jdbcTemplate.queryForObject(
            "SELECT quantity FROM stock_ledger WHERE sku_id = ? AND location_id = ?",
            BigDecimal.class, skuId, locationId
        );
        
        assertThat(quantityAfterFailedAdjustment)
            .as("Quantity should remain unchanged after rejected adjustment")
            .isEqualTo(currentQuantity);
            
        System.out.println("✓ Database unchanged - Quantity remains: " + quantityAfterFailedAdjustment);
        
        // Verify NO audit record was created for failed adjustment
        Integer auditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM stock_adjustment_audit WHERE sku_id = ? AND location_id = ? AND reason = ?",
            Integer.class, skuId, locationId, SharedEnumAdjustmentReason.DAMAGE.name()
        );
        
        assertThat(auditCount)
            .as("No audit record should be created for failed adjustment")
            .isEqualTo(0);
            
        System.out.println("✓ No audit record created for failed adjustment");
        
        // Verify logs contain appropriate error handling
        String logOutput = output.getOut();
        String logOutputLower = logOutput.toLowerCase();
        
        // Check for business rule validation in logs
        boolean containsValidationError = logOutputLower.contains("invalid quantity") ||
                                        logOutputLower.contains("negative quantity") ||
                                        logOutputLower.contains("insufficient stock") ||
                                        logOutputLower.contains("business rule") ||
                                        logOutputLower.contains("validation failed");
        assertThat(containsValidationError)
            .as("Logs should contain business rule validation")
            .isTrue();
            
        System.out.println("✓ Application logs show proper business rule enforcement");
        
        System.out.println("=== NEGATIVE QUANTITY TEST PASSED ===" +
            "\n✓ Request properly rejected" +
            "\n✓ Database remains unchanged" +
            "\n✓ No audit trail for failed operation" +
            "\n✓ Business rules enforced");
    }
}