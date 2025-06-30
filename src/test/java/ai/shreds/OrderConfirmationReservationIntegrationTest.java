package ai.shreds;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.value_objects.DomainEnumReservationStatus;
import ai.shreds.shared.dtos.SharedOrderConfirmedEventDTO;
import ai.shreds.shared.dtos.SharedReservationConfirmedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@ContextConfiguration(classes = OrderConfirmationReservationIntegrationTest.TestConfig.class)
public class OrderConfirmationReservationIntegrationTest extends BaseIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean(name = "stockValidationExecutor")
        public TaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @MockBean
    private ApplicationEventPublisherOutputPort kafkaEventPublisher;

    private static final String SKU_ID = "SKU-ORDER-01";
    private static final String LOCATION_ID = "WH-ORDER-01";
    private static final String ORDER_ID = "ORDER-12345";

    private UUID reservationId1;
    private UUID reservationId2;

    @BeforeEach
    void setupTestData() {
        reset(kafkaEventPublisher);
        
        // Generate reservation IDs
        reservationId1 = UUID.randomUUID();
        reservationId2 = UUID.randomUUID();
        
        // Clean up existing data
        jdbcTemplate.update("DELETE FROM reservation WHERE sku_id = ?", SKU_ID);
        jdbcTemplate.update("DELETE FROM stock_ledger WHERE sku_id = ?", SKU_ID);
        jdbcTemplate.update("DELETE FROM sku WHERE sku_id = ?", SKU_ID);
        jdbcTemplate.update("DELETE FROM location WHERE location_id = ?", LOCATION_ID);

        // Setup SKU and location
        jdbcTemplate.update("INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) VALUES (?, 'PROD-ORDER-01', 'V-ORDER-01', true, NOW(), NOW())", SKU_ID);
        jdbcTemplate.update("INSERT INTO location (location_id, name, type, is_active, created_at) VALUES (?, 'Order Test Warehouse', 'WAREHOUSE', true, NOW())", LOCATION_ID);
        
        // Setup stock ledger
        jdbcTemplate.update("INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, last_updated, version) VALUES (?, ?, ?, 100, 0, NOW(), 0)", UUID.randomUUID(), SKU_ID, LOCATION_ID);
        
        // Create pending reservations
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        jdbcTemplate.update(
            "INSERT INTO reservation (reservation_id, sku_id, location_id, quantity, status, expires_at, created_at, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            reservationId1, SKU_ID, LOCATION_ID, new BigDecimal("5.0000"), "PENDING", expiresAt, Instant.now(), "Order confirmation test"
        );
        jdbcTemplate.update(
            "INSERT INTO reservation (reservation_id, sku_id, location_id, quantity, status, expires_at, created_at, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            reservationId2, SKU_ID, LOCATION_ID, new BigDecimal("3.0000"), "PENDING", expiresAt, Instant.now(), "Order confirmation test"
        );
    }

    @Test
    void When_Order_Confirmed_Event_Received_Then_Reservations_Status_Changed_To_Confirmed(CapturedOutput output) {
        // Given
        List<String> reservationIds = Arrays.asList(reservationId1.toString(), reservationId2.toString());
        SharedOrderConfirmedEventDTO orderConfirmedEvent = new SharedOrderConfirmedEventDTO(
            ORDER_ID,
            reservationIds,
            Instant.now().toString()
        );
        
        // Verify initial state - reservations should be PENDING
        List<Map<String, Object>> initialReservations = jdbcTemplate.queryForList(
            "SELECT * FROM reservation WHERE reservation_id IN (?, ?) ORDER BY reservation_id", 
            reservationId1, reservationId2
        );
        assertThat(initialReservations).hasSize(2);
        assertThat(initialReservations.get(0).get("status")).isEqualTo("PENDING");
        assertThat(initialReservations.get(1).get("status")).isEqualTo("PENDING");

        // When
        eventPublisher.publishEvent(orderConfirmedEvent);

        // Then
        await().atMost(15, SECONDS).untilAsserted(() -> {
            // Verify reservations are now CONFIRMED
            List<Map<String, Object>> updatedReservations = jdbcTemplate.queryForList(
                "SELECT * FROM reservation WHERE reservation_id IN (?, ?) ORDER BY reservation_id", 
                reservationId1, reservationId2
            );
            assertThat(updatedReservations).hasSize(2);
            assertThat(updatedReservations.get(0).get("status")).isEqualTo(DomainEnumReservationStatus.CONFIRMED.name());
            assertThat(updatedReservations.get(1).get("status")).isEqualTo(DomainEnumReservationStatus.CONFIRMED.name());
            
            // Verify reservation confirmed events were published
            ArgumentCaptor<SharedReservationConfirmedEventDTO> captor = ArgumentCaptor.forClass(SharedReservationConfirmedEventDTO.class);
            verify(kafkaEventPublisher, times(2)).publishReservationConfirmed(captor.capture());
            
            List<SharedReservationConfirmedEventDTO> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).hasSize(2);
            
            // Verify first confirmed event
            SharedReservationConfirmedEventDTO firstEvent = publishedEvents.stream()
                .filter(event -> event.getReservationId().equals(reservationId1.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First reservation confirmed event not found"));
            
            assertThat(firstEvent.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(firstEvent.getSkuId()).isEqualTo(SKU_ID);
            assertThat(firstEvent.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(firstEvent.getStatus()).isEqualTo("CONFIRMED");
            assertThat(firstEvent.getConfirmedAt()).isNotNull();
            
            // Verify second confirmed event
            SharedReservationConfirmedEventDTO secondEvent = publishedEvents.stream()
                .filter(event -> event.getReservationId().equals(reservationId2.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Second reservation confirmed event not found"));
            
            assertThat(secondEvent.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(secondEvent.getSkuId()).isEqualTo(SKU_ID);
            assertThat(secondEvent.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(secondEvent.getStatus()).isEqualTo("CONFIRMED");
            assertThat(secondEvent.getConfirmedAt()).isNotNull();
            
            // Check logs
            assertThat(output).contains("Processing order confirmation for orderId: " + ORDER_ID);
            assertThat(output).contains("Reservation " + reservationId1 + " confirmed for order " + ORDER_ID);
            assertThat(output).contains("Reservation " + reservationId2 + " confirmed for order " + ORDER_ID);
        });
    }
}
