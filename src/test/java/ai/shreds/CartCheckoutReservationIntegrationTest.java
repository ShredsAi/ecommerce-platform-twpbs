package ai.shreds;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.value_objects.DomainEnumReservationStatus;
import ai.shreds.shared.dtos.CartItemDTO;
import ai.shreds.shared.dtos.SharedCartCheckoutEvent;
import ai.shreds.shared.dtos.SharedReservationCreatedEventDTO;
import ai.shreds.shared.dtos.SharedReservationFailedEventDTO;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@ContextConfiguration(classes = CartCheckoutReservationIntegrationTest.TestConfig.class)
public class CartCheckoutReservationIntegrationTest extends BaseIntegrationTest {

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

    private static final String SKU_ID = "SKU-CART-01";
    private static final String LOCATION_ID = "WH-CART-01";
    private static final String SKU_ID_NO_STOCK = "SKU-CART-02";
    private static final String LOCATION_ID_NO_STOCK = "WH-CART-02";

    @BeforeEach
    void setupTestData() {
        reset(kafkaEventPublisher);
        
        // Clean up existing data
        jdbcTemplate.update("DELETE FROM reservation WHERE sku_id IN (?, ?)", SKU_ID, SKU_ID_NO_STOCK);
        jdbcTemplate.update("DELETE FROM stock_ledger WHERE sku_id IN (?, ?)", SKU_ID, SKU_ID_NO_STOCK);
        jdbcTemplate.update("DELETE FROM sku WHERE sku_id IN (?, ?)", SKU_ID, SKU_ID_NO_STOCK);
        jdbcTemplate.update("DELETE FROM location WHERE location_id IN (?, ?)", LOCATION_ID, LOCATION_ID_NO_STOCK);

        // Setup SKUs and locations
        jdbcTemplate.update("INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) VALUES (?, 'PROD-CART-01', 'V-CART-01', true, NOW(), NOW())", SKU_ID);
        jdbcTemplate.update("INSERT INTO sku (sku_id, product_id, vendor_sku, is_active, created_at, updated_at) VALUES (?, 'PROD-CART-02', 'V-CART-02', true, NOW(), NOW())", SKU_ID_NO_STOCK);
        jdbcTemplate.update("INSERT INTO location (location_id, name, type, is_active, created_at) VALUES (?, 'Cart Test Warehouse', 'WAREHOUSE', true, NOW())", LOCATION_ID);
        jdbcTemplate.update("INSERT INTO location (location_id, name, type, is_active, created_at) VALUES (?, 'Cart Test Warehouse 2', 'WAREHOUSE', true, NOW())", LOCATION_ID_NO_STOCK);
        
        // Setup stock ledgers - one with sufficient stock, one with insufficient stock
        jdbcTemplate.update("INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, last_updated, version) VALUES (?, ?, ?, 100, 0, NOW(), 0)", UUID.randomUUID(), SKU_ID, LOCATION_ID);
        jdbcTemplate.update("INSERT INTO stock_ledger (ledger_id, sku_id, location_id, quantity, reserved, last_updated, version) VALUES (?, ?, ?, 5, 0, NOW(), 0)", UUID.randomUUID(), SKU_ID_NO_STOCK, LOCATION_ID_NO_STOCK);
    }

    @Test
    void whenCartCheckoutInitiated_withAvailableStock_thenReservationsAreCreated(CapturedOutput output) {
        // Given
        UUID cartId = UUID.randomUUID();
        CartItemDTO cartItem = new CartItemDTO(SKU_ID, LOCATION_ID, new BigDecimal("10"));
        SharedCartCheckoutEvent checkoutEvent = new SharedCartCheckoutEvent(cartId, UUID.randomUUID(), Collections.singletonList(cartItem));

        // When
        eventPublisher.publishEvent(checkoutEvent);

        // Then
        await().atMost(15, SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT * FROM reservation WHERE sku_id = ? AND location_id = ?", SKU_ID, LOCATION_ID
            );
            assertThat(reservations).hasSize(1);

            Map<String, Object> reservation = reservations.get(0);
            assertThat(reservation.get("sku_id")).isEqualTo(SKU_ID);
            assertThat(reservation.get("location_id")).isEqualTo(LOCATION_ID);
            // Ensure scale matches database NUMERIC(19,4)
            assertThat((BigDecimal) reservation.get("quantity")).isEqualByComparingTo(new BigDecimal("10.0000"));
            assertThat(reservation.get("status")).isEqualTo(DomainEnumReservationStatus.PENDING.name());
            
            // Verify success event was published
            ArgumentCaptor<SharedReservationCreatedEventDTO> captor = ArgumentCaptor.forClass(SharedReservationCreatedEventDTO.class);
            verify(kafkaEventPublisher, timeout(5000)).publishReservationCreated(captor.capture());
            
            SharedReservationCreatedEventDTO createdEvent = captor.getValue();
            assertThat(createdEvent.getCartId()).isEqualTo(cartId.toString());
            assertThat(createdEvent.getStatus()).isEqualTo("PENDING");
            
            // Check logs
            assertThat(output).contains("Successfully created reservations for cartId: " + cartId);
        });
    }
    
    @Test
    void whenCartCheckoutInitiated_withInsufficientStock_thenNoReservationsCreatedAndFailedEventPublished(CapturedOutput output) {
        // Given - cart with item requesting more than available stock
        UUID cartId = UUID.randomUUID();
        CartItemDTO cartItem = new CartItemDTO(SKU_ID_NO_STOCK, LOCATION_ID_NO_STOCK, new BigDecimal("10")); // Requesting 10 but only 5 available
        SharedCartCheckoutEvent checkoutEvent = new SharedCartCheckoutEvent(cartId, UUID.randomUUID(), Collections.singletonList(cartItem));

        // When
        eventPublisher.publishEvent(checkoutEvent);

        // Then
        await().atMost(15, SECONDS).untilAsserted(() -> {
            // Verify no reservations were created
            List<Map<String, Object>> reservations = jdbcTemplate.queryForList(
                "SELECT * FROM reservation WHERE sku_id = ? AND location_id = ?", SKU_ID_NO_STOCK, LOCATION_ID_NO_STOCK
            );
            assertThat(reservations).isEmpty();
            
            // Verify failure event was published
            ArgumentCaptor<SharedReservationFailedEventDTO> captor = ArgumentCaptor.forClass(SharedReservationFailedEventDTO.class);
            verify(kafkaEventPublisher, timeout(5000)).publishReservationFailed(captor.capture());
            
            SharedReservationFailedEventDTO failedEvent = captor.getValue();
            assertThat(failedEvent.getCartId()).isEqualTo(cartId.toString());
            assertThat(failedEvent.getError()).isEqualTo("INSUFFICIENT_STOCK");
            assertThat(failedEvent.getFailedItems()).hasSize(1);
            
            SharedReservationFailedEventDTO.SharedFailedItemDTO failedItem = failedEvent.getFailedItems().get(0);
            assertThat(failedItem.getSkuId()).isEqualTo(SKU_ID_NO_STOCK);
            assertThat(failedItem.getLocationId()).isEqualTo(LOCATION_ID_NO_STOCK);
            assertThat(failedItem.getRequestedQuantity()).isEqualTo(10);
            assertThat(failedItem.getAvailableQuantity()).isEqualTo(5);
            
            // Verify no success event was published
            verify(kafkaEventPublisher, never()).publishReservationCreated(any());
            
            // Check logs
            assertThat(output).contains("Failed to reserve items for cartId: " + cartId);
            assertThat(output).contains("Not all items were available");
        });
    }
}
