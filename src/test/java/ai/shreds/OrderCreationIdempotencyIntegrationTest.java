package ai.shreds;

import ai.shreds.application.ports.ApplicationInventoryOutputPort;
import ai.shreds.application.ports.ApplicationPricingOutputPort;
import ai.shreds.application.dtos.*;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.infrastructure.repositories.InfrastructureOrderJpaRepository;
import ai.shreds.shared.dtos.*;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for Order Creation idempotency handling.
 * Tests that duplicate CartCheckedOut events do not create duplicate orders.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "logging.level.ai.shreds=DEBUG",
        "logging.level.org.springframework.transaction=DEBUG",
        "logging.level.org.testcontainers=INFO",
        "spring.kafka.listener.ack-mode=manual_immediate",
        "spring.kafka.consumer.enable-auto-commit=false"
    }
)
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@EmbeddedKafka(
    partitions = 1,
    topics = {"shopping-cart-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",
        "port=0"
    }
)
@DirtiesContext
@Import(OrderCreationIdempotencyIntegrationTest.TestEventListenerConfig.class)
class OrderCreationIdempotencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2-alpine"))
            .withDatabaseName("test_orders")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(false);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InfrastructureOrderJpaRepository orderJpaRepository;
    
    @Autowired
    private TestEventListener testEventListener;
    
    // Mock external gRPC services
    @MockBean
    private ApplicationInventoryOutputPort inventoryOutputPort;
    
    @MockBean
    private ApplicationPricingOutputPort pricingOutputPort;
    
    // Mock gRPC channels to avoid connectivity issues
    @MockBean(name = "inventoryServiceChannel")
    private ManagedChannel inventoryServiceChannel;
    
    @MockBean(name = "pricingServiceChannel")
    private ManagedChannel pricingServiceChannel;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Configure dummy gRPC addresses
        registry.add("grpc.client.inventory.address", () -> "localhost:50091");
        registry.add("grpc.client.pricing.address", () -> "localhost:50092");
        
        // Disable health checks
        registry.add("management.health.db.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
        
        // Configure shorter timeouts for faster test execution
        registry.add("spring.transaction.default-timeout", () -> "10s");
        registry.add("resilience4j.retry.instances.inventory-service.max-attempts", () -> "1");
        registry.add("resilience4j.retry.instances.pricing-service.max-attempts", () -> "1");
    }

    @Test
    void When_Duplicate_CartCheckedOut_Event_Consumed_Then_Existing_Order_Returned_Without_Duplication() throws Exception {
        // Arrange
        System.out.println("=== STARTING IDEMPOTENCY INTEGRATION TEST ===");
        
        setupSuccessfulMockResponses();
        
        // Create a cart event with a static cartId for idempotency testing
        String staticCartId = "idempotency-test-cart-123";
        SharedCartCheckedOutEventDTO cartEvent = createCartCheckedOutEventWithCartId(staticCartId);
        String eventJson = objectMapper.writeValueAsString(cartEvent);
        
        System.out.println("Cart Event JSON: " + eventJson);
        System.out.println("Using static cartId for idempotency test: " + staticCartId);
        
        // Clear database and reset event listener
        orderJpaRepository.deleteAll();
        testEventListener.reset();
        
        // Act - Send the first CartCheckedOut event
        System.out.println("Sending first CartCheckedOut event to Kafka topic 'shopping-cart-events'");
        kafkaTemplate.send("shopping-cart-events", cartEvent.getCartId(), eventJson).get(10, TimeUnit.SECONDS);
        
        // Assert - Wait for first processing and verify first order creation
        System.out.println("Waiting for first order processing...");
        
        // Wait for first order to be persisted
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<DomainOrderEntity> orders = orderJpaRepository.findAll();
                System.out.println("Orders after first event: " + orders.size());
                
                assertThat(orders)
                    .hasSize(1)
                    .extracting(DomainOrderEntity::getCartId)
                    .contains(staticCartId);
            });
        
        // Wait for first OrderCreated event to be published
        System.out.println("Waiting for first OrderCreated event to be published...");
        boolean firstEventReceived = testEventListener.awaitOrderCreatedEvent(15, TimeUnit.SECONDS);
        assertThat(firstEventReceived)
            .as("Should receive first OrderCreated event within timeout")
            .isTrue();
        
        // Get the first order details
        List<DomainOrderEntity> ordersAfterFirst = orderJpaRepository.findAll();
        assertThat(ordersAfterFirst).hasSize(1);
        DomainOrderEntity firstOrder = ordersAfterFirst.get(0);
        
        System.out.println("First order created successfully:");
        System.out.println("  Order ID: " + firstOrder.getOrderId());
        System.out.println("  Order Number: " + firstOrder.getOrderNumber());
        System.out.println("  Cart ID: " + firstOrder.getCartId());
        System.out.println("  Customer ID: " + firstOrder.getCustomerId());
        System.out.println("  Total Amount: " + firstOrder.getTotalAmount());
        
        // Verify first event details
        List<SharedOrderCreatedEventDTO> firstEvents = testEventListener.getOrderCreatedEvents();
        assertThat(firstEvents).hasSize(1);
        SharedOrderCreatedEventDTO firstPublishedEvent = firstEvents.get(0);
        
        // Reset event listener for second event
        testEventListener.resetForSecondEvent();
        
        // Act - Send the SAME CartCheckedOut event again (duplicate)
        System.out.println("\n=== SENDING DUPLICATE EVENT ===");
        System.out.println("Sending duplicate CartCheckedOut event with same cartId: " + staticCartId);
        kafkaTemplate.send("shopping-cart-events", cartEvent.getCartId(), eventJson).get(10, TimeUnit.SECONDS);
        
        // Assert - Wait for duplicate processing and verify NO duplicate order is created
        System.out.println("Waiting for duplicate event processing...");
        
        // Wait a reasonable amount of time to allow duplicate processing
        Thread.sleep(5000); // 5 seconds should be enough for processing
        
        // Verify that still only one order exists in database
        List<DomainOrderEntity> ordersAfterDuplicate = orderJpaRepository.findAll();
        System.out.println("Orders after duplicate event: " + ordersAfterDuplicate.size());
        
        assertThat(ordersAfterDuplicate)
            .as("Should still have exactly one order after processing duplicate event")
            .hasSize(1);
        
        DomainOrderEntity orderAfterDuplicate = ordersAfterDuplicate.get(0);
        
        // Verify it's the same order (same ID, same details)
        assertThat(orderAfterDuplicate.getOrderId())
            .as("Order ID should be the same as the first order")
            .isEqualTo(firstOrder.getOrderId());
        
        assertThat(orderAfterDuplicate.getOrderNumber())
            .as("Order number should be the same as the first order")
            .isEqualTo(firstOrder.getOrderNumber());
        
        assertThat(orderAfterDuplicate.getCartId())
            .as("Cart ID should be the same")
            .isEqualTo(staticCartId);
        
        assertThat(orderAfterDuplicate.getCustomerId())
            .as("Customer ID should be the same")
            .isEqualTo(firstOrder.getCustomerId());
        
        assertThat(orderAfterDuplicate.getTotalAmount())
            .as("Total amount should be the same")
            .isEqualByComparingTo(firstOrder.getTotalAmount());
        
        assertThat(orderAfterDuplicate.getCreatedAt())
            .as("Created timestamp should be the same (no new order created)")
            .isEqualTo(firstOrder.getCreatedAt());
        
        System.out.println("Verified: Duplicate event did not create a new order");
        System.out.println("Same order details:");
        System.out.println("  Order ID: " + orderAfterDuplicate.getOrderId());
        System.out.println("  Order Number: " + orderAfterDuplicate.getOrderNumber());
        System.out.println("  Created At: " + orderAfterDuplicate.getCreatedAt());
        
        // Verify that either:
        // 1. No second OrderCreated event was published (preferred - duplicate ignored)
        // 2. OR if a second event was published, it has the same order details
        boolean secondEventReceived = testEventListener.awaitSecondOrderCreatedEvent(10, TimeUnit.SECONDS);
        
        List<SharedOrderCreatedEventDTO> allEvents = testEventListener.getAllOrderCreatedEvents();
        System.out.println("Total OrderCreated events captured: " + allEvents.size());
        
        if (secondEventReceived && allEvents.size() > 1) {
            System.out.println("Second event was published - verifying it has same order details");
            SharedOrderCreatedEventDTO secondEvent = allEvents.get(allEvents.size() - 1); // Get last event
            
            // If second event was published, it should have same order details
            assertThat(secondEvent.getOrderId())
                .as("Second event should reference same order ID")
                .isEqualTo(firstPublishedEvent.getOrderId());
            
            assertThat(secondEvent.getOrderNumber())
                .as("Second event should reference same order number")
                .isEqualTo(firstPublishedEvent.getOrderNumber());
        } else {
            System.out.println("No second event published - duplicate was properly ignored");
            assertThat(allEvents)
                .as("Should have only one event if duplicate was properly ignored")
                .hasSize(1);
        }
        
        // Verify no failure events were published
        List<SharedOrderCreationFailedEventDTO> failureEvents = testEventListener.getOrderCreationFailedEvents();
        assertThat(failureEvents)
            .as("Should not publish any failure events for idempotency handling")
            .isEmpty();
        
        System.out.println("=== IDEMPOTENCY TEST COMPLETED SUCCESSFULLY ===");
        System.out.println("✓ Duplicate CartCheckedOut event did not create duplicate order");
        System.out.println("✓ Same order was preserved with same ID, number, and creation timestamp");
        System.out.println("✓ No failure events were published");
        System.out.println("✓ Idempotency mechanism working correctly");
    }
    
    private void setupSuccessfulMockResponses() {
        System.out.println("Setting up successful mock responses for external services...");
        
        // Setup inventory service mock - all items available
        ApplicationInventoryResultDTO inventoryResult1 = new ApplicationInventoryResultDTO();
        inventoryResult1.setProductId("prod-001");
        inventoryResult1.setAvailable(true);
        inventoryResult1.setAvailableQty(10);
        
        ApplicationInventoryResultDTO inventoryResult2 = new ApplicationInventoryResultDTO();
        inventoryResult2.setProductId("prod-002");
        inventoryResult2.setAvailable(true);
        inventoryResult2.setAvailableQty(5);
        
        ApplicationInventoryCheckResponseDTO inventoryResponse = new ApplicationInventoryCheckResponseDTO();
        inventoryResponse.setResults(List.of(inventoryResult1, inventoryResult2));
        inventoryResponse.setAllAvailable(true);
        
        when(inventoryOutputPort.checkAvailability(any(ApplicationInventoryCheckRequestDTO.class)))
            .thenReturn(inventoryResponse);
        
        // Setup pricing service mock
        SharedMoneyDTO subtotal = new SharedMoneyDTO();
        subtotal.setAmount(new BigDecimal("100.00"));
        subtotal.setCurrency("USD");
        
        SharedMoneyDTO tax = new SharedMoneyDTO();
        tax.setAmount(new BigDecimal("10.00"));
        tax.setCurrency("USD");
        
        SharedMoneyDTO discounts = new SharedMoneyDTO();
        discounts.setAmount(BigDecimal.ZERO);
        discounts.setCurrency("USD");
        
        SharedMoneyDTO total = new SharedMoneyDTO();
        total.setAmount(new BigDecimal("110.00"));
        total.setCurrency("USD");
        
        ApplicationItemPricingDTO itemPricing1 = new ApplicationItemPricingDTO();
        itemPricing1.setProductId("prod-001");
        itemPricing1.setUnitPrice(new BigDecimal("49.99"));
        itemPricing1.setTotalPrice(new BigDecimal("99.98"));
        
        ApplicationItemPricingDTO itemPricing2 = new ApplicationItemPricingDTO();
        itemPricing2.setProductId("prod-002");
        itemPricing2.setUnitPrice(new BigDecimal("0.02"));
        itemPricing2.setTotalPrice(new BigDecimal("0.02"));
        
        ApplicationPricingResponseDTO pricingResponse = new ApplicationPricingResponseDTO();
        pricingResponse.setSubtotal(subtotal);
        pricingResponse.setTax(tax);
        pricingResponse.setDiscounts(discounts);
        pricingResponse.setTotal(total);
        pricingResponse.setItemBreakdown(List.of(itemPricing1, itemPricing2));
        
        when(pricingOutputPort.calculatePricing(any(ApplicationPricingRequestDTO.class)))
            .thenReturn(pricingResponse);
        
        System.out.println("Successful mock responses configured");
    }
    
    private SharedCartCheckedOutEventDTO createCartCheckedOutEventWithCartId(String cartId) {
        System.out.println("Creating CartCheckedOut event with cartId: " + cartId);
        
        SharedCartCheckedOutEventDTO event = new SharedCartCheckedOutEventDTO();
        event.setCustomerId("cust-idempotency-test");
        event.setCartId(cartId);
        event.setTimestamp(Instant.now());
        
        // Create cart items
        SharedCartItemDTO item1 = new SharedCartItemDTO("prod-001", 2, "cart-item-1");
        SharedCartItemDTO item2 = new SharedCartItemDTO("prod-002", 1, "cart-item-2");
        event.setItems(List.of(item1, item2));
        
        // Create billing address
        SharedAddressDTO billingAddress = new SharedAddressDTO();
        billingAddress.setStreet1("123 Idempotency St");
        billingAddress.setStreet2("Apt 1A");
        billingAddress.setCity("Test City");
        billingAddress.setState("TS");
        billingAddress.setPostalCode("12345");
        billingAddress.setCountry("US");
        event.setBillingAddress(billingAddress);
        
        // Create shipping address
        SharedAddressDTO shippingAddress = new SharedAddressDTO();
        shippingAddress.setStreet1("456 Duplicate Ave");
        shippingAddress.setCity("Test City");
        shippingAddress.setState("TS");
        shippingAddress.setPostalCode("54321");
        shippingAddress.setCountry("US");
        event.setShippingAddress(shippingAddress);
        
        // Create payment method
        SharedPaymentMethodDTO paymentMethod = new SharedPaymentMethodDTO();
        paymentMethod.setType("CREDIT_CARD");
        paymentMethod.setProvider("test-provider");
        paymentMethod.setDetails(Map.of(
            "cardLast4", "1234",
            "cardBrand", "test",
            "paymentIntentId", "pi_idempotency_test"
        ));
        event.setPaymentMethod(paymentMethod);
        
        return event;
    }
    
    /**
     * Test configuration to register the TestEventListener bean.
     */
    @TestConfiguration
    static class TestEventListenerConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }
    
    /**
     * Enhanced test event listener for idempotency testing.
     */
    static class TestEventListener {
        private final List<SharedOrderCreatedEventDTO> orderCreatedEvents = new ArrayList<>();
        private final List<SharedOrderCreationFailedEventDTO> orderCreationFailedEvents = new ArrayList<>();
        private CountDownLatch orderCreatedLatch = new CountDownLatch(1);
        private CountDownLatch secondOrderCreatedLatch = new CountDownLatch(1);
        private CountDownLatch orderCreationFailedLatch = new CountDownLatch(1);
        private int eventCount = 0;
        
        @EventListener
        public void handleOrderCreatedEvent(SharedOrderCreatedEventDTO event) {
            synchronized (this) {
                eventCount++;
                System.out.println("TestEventListener captured OrderCreated event #" + eventCount + ": " + event.getOrderId());
                orderCreatedEvents.add(event);
                
                if (eventCount == 1) {
                    orderCreatedLatch.countDown();
                } else if (eventCount == 2) {
                    secondOrderCreatedLatch.countDown();
                }
            }
        }
        
        @EventListener
        public void handleOrderCreationFailedEvent(SharedOrderCreationFailedEventDTO event) {
            System.out.println("TestEventListener captured OrderCreationFailed event: " + event.getCartId());
            orderCreationFailedEvents.add(event);
            orderCreationFailedLatch.countDown();
        }
        
        public boolean awaitOrderCreatedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderCreatedLatch.await(timeout, unit);
        }
        
        public boolean awaitSecondOrderCreatedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return secondOrderCreatedLatch.await(timeout, unit);
        }
        
        public boolean awaitOrderCreationFailedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderCreationFailedLatch.await(timeout, unit);
        }
        
        public List<SharedOrderCreatedEventDTO> getOrderCreatedEvents() {
            return new ArrayList<>(orderCreatedEvents);
        }
        
        public List<SharedOrderCreatedEventDTO> getAllOrderCreatedEvents() {
            return new ArrayList<>(orderCreatedEvents);
        }
        
        public List<SharedOrderCreationFailedEventDTO> getOrderCreationFailedEvents() {
            return new ArrayList<>(orderCreationFailedEvents);
        }
        
        public void reset() {
            orderCreatedEvents.clear();
            orderCreationFailedEvents.clear();
            orderCreatedLatch = new CountDownLatch(1);
            secondOrderCreatedLatch = new CountDownLatch(1);
            orderCreationFailedLatch = new CountDownLatch(1);
            eventCount = 0;
        }
        
        public void resetForSecondEvent() {
            // Keep existing events but reset latches for second event
            secondOrderCreatedLatch = new CountDownLatch(1);
        }
    }
}