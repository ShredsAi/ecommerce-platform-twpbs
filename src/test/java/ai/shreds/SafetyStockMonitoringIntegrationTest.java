package ai.shreds;

import ai.shreds.shared.dtos.*;
import ai.shreds.shared.enums.SharedEnumAlertStatus;
import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.application.ports.ApplicationInventoryChangeNotificationOutputPort;
import ai.shreds.application.ports.ApplicationSafetyStockMonitorInputPort;
import ai.shreds.infrastructure.external_services.InfrastructureKafkaEventPublisher;
import ai.shreds.adapter.primary.AdapterSafetyStockMonitorScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

@SpringBootTest(
    classes = InventoryTrackingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.level.ai.shreds=DEBUG",
    "logging.level.org.springframework.boot=INFO",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.h2.console.enabled=true",
    "spring.sql.init.mode=never",
    "management.health.redis.enabled=false",
    "management.health.kafka.enabled=false",
    "management.health.jms.enabled=false",
    "management.metrics.cache.instrument=false",
    "management.metrics.enabled=false",
    "cache.ttl=10",
    "spring.redis.timeout=2000",
    "spring.redis.host=localhost",
    "spring.redis.port=6370",
    "spring.kafka.bootstrap-servers=localhost:9093",
    "spring.activemq.in-memory=true",
    "spring.task.scheduling.enabled=false",
    "inventory.safety-stock.monitor-interval=30000",
    "inventory.safety-stock.batch-size=100"
})
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig
public class SafetyStockMonitoringIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationSafetyStockMonitorInputPort safetyStockMonitorPort;

    @Autowired
    private AdapterSafetyStockMonitorScheduler safetyStockScheduler;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private ApplicationCacheOutputPort cacheOutputPort;

    @MockBean
    private ApplicationInventoryChangeNotificationOutputPort inventoryChangeNotificationOutputPort;

    @MockBean
    private InfrastructureKafkaEventPublisher infrastructureKafkaEventPublisher;

    @MockBean
    private KafkaTemplate<String, SharedInventoryChangedEvent> inventoryEventKafkaTemplate;

    @MockBean
    private KafkaTemplate<String, SharedLowStockAlertEvent> lowStockAlertKafkaTemplate;

    @MockBean
    private KafkaTemplate<String, Object> genericKafkaTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            setupTestDataForSafetyStockMonitoring();
            return null;
        });
    }

    private void setupTestDataForSafetyStockMonitoring() {
        try {
            jdbcTemplate.execute("DELETE FROM low_stock_alert");
            jdbcTemplate.execute("DELETE FROM outbox_event");
            jdbcTemplate.execute("DELETE FROM safety_stock_rule");
            jdbcTemplate.execute("DELETE FROM stock_ledger");
            jdbcTemplate.execute("DELETE FROM sku");
            jdbcTemplate.execute("DELETE FROM location");

            jdbcTemplate.execute(
                "INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) " +
                "VALUES ('SKULOW001', 'PRODLOW001', 'VENDORSKULOW001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
            jdbcTemplate.execute(
                "INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) " +
                "VALUES ('SKUNORMAL001', 'PRODNORMAL001', 'VENDORSKUNORMAL001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );

            jdbcTemplate.execute(
                "INSERT INTO location (location_id, name, type, is_active, created_at) " +
                "VALUES ('WHMONITORING001', 'Monitoring Warehouse 1', 'WAREHOUSE', true, CURRENT_TIMESTAMP)"
            );
            jdbcTemplate.execute(
                "INSERT INTO location (location_id, name, type, is_active, created_at) " +
                "VALUES ('WHMONITORING002', 'Monitoring Warehouse 2', 'WAREHOUSE', true, CURRENT_TIMESTAMP)"
            );

            UUID lowStockLedgerId = UUID.randomUUID();
            UUID normalStockLedgerId = UUID.randomUUID();

            jdbcTemplate.execute(
                "INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, available, version, last_updated) " +
                "VALUES ('" + lowStockLedgerId + "', 'SKULOW001', 'WHMONITORING001', 5.0000, 0.0000, 5.0000, 0, CURRENT_TIMESTAMP)"
            );

            jdbcTemplate.execute(
                "INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, available, version, last_updated) " +
                "VALUES ('" + normalStockLedgerId + "', 'SKUNORMAL001', 'WHMONITORING002', 50.0000, 0.0000, 50.0000, 0, CURRENT_TIMESTAMP)"
            );

            UUID lowStockRuleId = UUID.randomUUID();
            UUID normalStockRuleId = UUID.randomUUID();

            jdbcTemplate.execute(
                "INSERT INTO safety_stock_rule (rule_id, sku_id, location_id, min_quantity, is_active, created_at, updated_at) " +
                "VALUES ('" + lowStockRuleId + "', 'SKULOW001', 'WHMONITORING001', 10.0000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );

            jdbcTemplate.execute(
                "INSERT INTO safety_stock_rule (rule_id, sku_id, location_id, min_quantity, is_active, created_at, updated_at) " +
                "VALUES ('" + normalStockRuleId + "', 'SKUNORMAL001', 'WHMONITORING002', 10.0000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );

        } catch (Exception e) {
            System.err.println("Error in setupTestDataForSafetyStockMonitoring: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void When_Stock_Level_Falls_Below_Safety_Threshold_Then_Low_Stock_Alert_Is_Generated_And_Published_To_Kafka(CapturedOutput output) {
        System.out.println("=== SAFETY STOCK MONITORING INTEGRATION TEST ===");

        transactionTemplate.execute(status -> {
            safetyStockMonitorPort.checkAndGenerateAlerts();
            return null;
        });

        System.out.println("Verifying database changes...");

        List<Map<String, Object>> alertDetails = jdbcTemplate.queryForList(
            "SELECT alert_id, sku_id, location_id, current_quantity, threshold, status FROM low_stock_alert " +
            "WHERE sku_id = 'SKULOW001' AND location_id = 'WHMONITORING001'"
        );

        assertThat(alertDetails)
            .as("Should have exactly one alert record for SKULOW001")
            .hasSize(1);

        Map<String, Object> alert = alertDetails.get(0);
        assertThat(alert.get("sku_id")).isEqualTo("SKULOW001");
        assertThat(alert.get("location_id")).isEqualTo("WHMONITORING001");
        assertThat((BigDecimal) alert.get("current_quantity")).isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat((BigDecimal) alert.get("threshold")).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(alert.get("status")).isEqualTo(SharedEnumAlertStatus.PENDING.name());

        // Fix for ClassCastException: retrieve as UUID and convert to String
        Object alertIdObject = alert.get("alert_id");
        assertThat(alertIdObject).isInstanceOf(UUID.class);
        String alertId = alertIdObject.toString();
        assertThat(alertId).isNotNull().isNotBlank();

        Integer normalStockAlertCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM low_stock_alert WHERE sku_id = 'SKUNORMAL001'",
            Integer.class
        );
        assertThat(normalStockAlertCount)
            .as("No alert should be created for SKU with sufficient stock")
            .isEqualTo(0);

        Integer outboxEventCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type = 'LOW_STOCK_ALERT' AND processed = false",
            Integer.class
        );
        assertThat(outboxEventCount)
            .as("Outbox event should be created for low stock alert publishing")
            .isEqualTo(1);

        verify(infrastructureKafkaEventPublisher, atLeastOnce()).publishLowStockAlert(any(SharedLowStockAlertEvent.class));

        System.out.println("✓ Safety stock monitoring test passed.");
    }
}