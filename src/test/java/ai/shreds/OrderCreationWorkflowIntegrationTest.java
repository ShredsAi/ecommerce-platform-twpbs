package ai.shreds;

import ai.shreds.application.ports.ApplicationInventoryOutputPort;
import ai.shreds.application.ports.ApplicationPricingOutputPort;
import ai.shreds.application.dtos.*;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.infrastructure.repositories.InfrastructureOrderJpaRepository;
import ai.shreds.shared.dtos.*;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedErrorTypeEnum;
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
import org.springframework.stereotype.Component;
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
 * Integration test for the complete Order Creation workflow.
 * Tests end-to-end flow from CartCheckedOut Kafka event consumption to Order persistence and OrderCreated event publishing.
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
@Import(OrderCreationWorkflowIntegrationTest.TestEventListenerConfig.class)
class OrderCreationWorkflowIntegrationTest {

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
    void When_Valid_CartCheckedOut_Event_Consumed_Then_Order_Created_And_Persisted_And_Event_Published() throws Exception {
        // Arrange
        System.out.println("=== STARTING ORDER CREATION WORKFLOW INTEGRATION TEST ===");
        
        setupSuccessfulMockResponses();
        
        SharedCartCheckedOutEventDTO cartEvent = createValidCartCheckedOutEvent();
        String eventJson = objectMapper.writeValueAsString(cartEvent);
        
        System.out.println("Cart Event JSON: " + eventJson);
        
        // Reset event listener
        testEventListener.reset();
        
        // Act - Send Kafka message
        System.out.println("Sending CartCheckedOut event to Kafka topic 'shopping-cart-events'");
        kafkaTemplate.send("shopping-cart-events", cartEvent.getCartId(), eventJson).get(10, TimeUnit.SECONDS);
        
        // Assert - Wait for processing and verify results
        System.out.println("Waiting for order processing...");
        
        // Wait for order to be persisted
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<DomainOrderEntity> orders = orderJpaRepository.findAll();
                System.out.println("Current orders in database: " + orders.size());
                
                assertThat(orders)
                    .hasSize(1)
                    .extracting(DomainOrderEntity::getCartId)
                    .contains(cartEvent.getCartId());
                
                DomainOrderEntity savedOrder = orders.get(0);
                
                // Verify order properties
                assertThat(savedOrder.getCustomerId()).isEqualTo(cartEvent.getCustomerId());
                assertThat(savedOrder.getCartId()).isEqualTo(cartEvent.getCartId());
                assertThat(savedOrder.getOrderStatus()).isEqualTo(SharedOrderStatusEnum.PENDING);
                assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("110.00"));
                assertThat(savedOrder.getCurrency()).isEqualTo("USD");
                assertThat(savedOrder.getOrderNumber()).isNotNull().isNotBlank();
                assertThat(savedOrder.getOrderId()).isNotNull();
                
                System.out.println("Order successfully persisted with ID: " + savedOrder.getOrderId());
                System.out.println("Order Number: " + savedOrder.getOrderNumber());
                System.out.println("Order Status: " + savedOrder.getOrderStatus());
                System.out.println("Order Total: " + savedOrder.getTotalAmount() + " " + savedOrder.getCurrency());
            });
        
        // Wait for Spring Application Event to be published
        System.out.println("Waiting for OrderCreated event to be published...");
        
        boolean eventReceived = testEventListener.awaitOrderCreatedEvent(15, TimeUnit.SECONDS);
        assertThat(eventReceived)
            .as("Should receive OrderCreated event within timeout")
            .isTrue();
        
        // Get captured events
        List<SharedOrderCreatedEventDTO> orderCreatedEvents = testEventListener.getOrderCreatedEvents();
        System.out.println("OrderCreated events captured: " + orderCreatedEvents.size());
        
        assertThat(orderCreatedEvents)
            .as("Should publish exactly one OrderCreated event")
            .hasSize(1);
        
        SharedOrderCreatedEventDTO publishedEvent = orderCreatedEvents.get(0);
        
        // Verify event properties
        assertThat(publishedEvent.getCustomerId()).isEqualTo(cartEvent.getCustomerId());
        assertThat(publishedEvent.getOrderStatus()).isEqualTo(SharedOrderStatusEnum.PENDING);
        assertThat(publishedEvent.getTotalAmount().getAmount()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(publishedEvent.getTotalAmount().getCurrency()).isEqualTo("USD");
        assertThat(publishedEvent.getItemCount()).isEqualTo(2);
        assertThat(publishedEvent.getOrderId()).isNotNull();
        assertThat(publishedEvent.getOrderNumber()).isNotNull();
        
        System.out.println("OrderCreated event successfully published with order ID: " + publishedEvent.getOrderId());
        
        // Verify no failure events were published
        List<SharedOrderCreationFailedEventDTO> failureEvents = testEventListener.getOrderCreationFailedEvents();
        assertThat(failureEvents)
            .as("Should not publish any failure events for successful order creation")
            .isEmpty();
        
        System.out.println("=== ORDER CREATION WORKFLOW TEST COMPLETED SUCCESSFULLY ===");
    }

    @Test
    void When_Inventory_Is_Unavailable_Then_Order_Creation_Fails_And_OrderCreationFailed_Event_Is_Published() throws Exception {
        // Arrange
        System.out.println("=== STARTING INVENTORY UNAVAILABILITY TEST ===");
        
        setupInventoryUnavailableMockResponses();
        
        SharedCartCheckedOutEventDTO cartEvent = createValidCartCheckedOutEvent();
        String eventJson = objectMapper.writeValueAsString(cartEvent);
        
        System.out.println("Cart Event JSON for inventory test: " + eventJson);
        
        // Reset event listener and clear any existing orders
        testEventListener.reset();
        orderJpaRepository.deleteAll();
        
        // Act - Send Kafka message
        System.out.println("Sending CartCheckedOut event with insufficient inventory to Kafka topic 'shopping-cart-events'");
        kafkaTemplate.send("shopping-cart-events", cartEvent.getCartId(), eventJson).get(10, TimeUnit.SECONDS);
        
        // Assert - Wait for processing and verify failure handling
        System.out.println("Waiting for inventory failure processing...");
        
        // Wait for OrderCreationFailed event to be published
        boolean failureEventReceived = testEventListener.awaitOrderCreationFailedEvent(30, TimeUnit.SECONDS);
        assertThat(failureEventReceived)
            .as("Should receive OrderCreationFailed event within timeout")
            .isTrue();
        
        // Verify no order was persisted
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<DomainOrderEntity> orders = orderJpaRepository.findAll();
                System.out.println("Orders in database after failure: " + orders.size());
                
                assertThat(orders)
                    .as("No order should be persisted when inventory is unavailable")
                    .isEmpty();
            });
        
        // Get captured failure events
        List<SharedOrderCreationFailedEventDTO> failureEvents = testEventListener.getOrderCreationFailedEvents();
        System.out.println("OrderCreationFailed events captured: " + failureEvents.size());
        
        assertThat(failureEvents)
            .as("Should publish exactly one OrderCreationFailed event")
            .hasSize(1);
        
        SharedOrderCreationFailedEventDTO failedEvent = failureEvents.get(0);
        
        // Debug: Print the actual event details
        System.out.println("Debug - Failed event details:");
        System.out.println("  cartId: " + (failedEvent != null ? failedEvent.getCartId() : "[failedEvent is null]"));
        System.out.println("  customerId: " + (failedEvent != null ? failedEvent.getCustomerId() : "[failedEvent is null]"));
        System.out.println("  errorType: " + (failedEvent != null ? failedEvent.getErrorType() : "[failedEvent is null]"));
        System.out.println("  errorMessage: " + (failedEvent != null ? failedEvent.getErrorMessage() : "[failedEvent is null]"));
        System.out.println("  failureReason: " + (failedEvent != null ? failedEvent.getFailureReason() : "[failedEvent is null]"));
        
        // Verify failure event properties - now with null checks
        assertThat(failedEvent).as("Failed event should not be null").isNotNull();
        
        assertThat(failedEvent.getCartId())
            .as("Failed event cartId should not be null")
            .isNotNull()
            .isEqualTo(cartEvent.getCartId());
            
        assertThat(failedEvent.getCustomerId()).isEqualTo(cartEvent.getCustomerId());
        assertThat(failedEvent.getErrorType()).isEqualTo(SharedErrorTypeEnum.INVENTORY_UNAVAILABLE);
        assertThat(failedEvent.getErrorMessage()).isNotNull().isNotBlank();
        assertThat(failedEvent.getFailureReason()).isNotNull().contains("inventory", "unavailable");
        assertThat(failedEvent.getTimestamp()).isNotNull();
        assertThat(failedEvent.getCorrelationId()).isEqualTo(cartEvent.getCartId());
        
        System.out.println("OrderCreationFailed event successfully published for cartId: " + failedEvent.getCartId());
        System.out.println("Error Type: " + failedEvent.getErrorType());
        System.out.println("Error Message: " + failedEvent.getErrorMessage());
        System.out.println("Failure Reason: " + failedEvent.getFailureReason());
        
        // Verify no success events were published
        List<SharedOrderCreatedEventDTO> successEvents = testEventListener.getOrderCreatedEvents();
        assertThat(successEvents)
            .as("Should not publish any success events when inventory is unavailable")
            .isEmpty();
        
        System.out.println("=== INVENTORY UNAVAILABILITY TEST COMPLETED SUCCESSFULLY ===");
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
        
        System.out.println("Successful mock responses configured successfully");
    }

    private void setupInventoryUnavailableMockResponses() {
        System.out.println("Setting up inventory unavailable mock responses...");
        
        // Setup inventory service mock - some items unavailable
        ApplicationInventoryResultDTO inventoryResult1 = new ApplicationInventoryResultDTO();
        inventoryResult1.setProductId("prod-001");
        inventoryResult1.setAvailable(false); // This item is unavailable
        inventoryResult1.setAvailableQty(0);
        
        ApplicationInventoryResultDTO inventoryResult2 = new ApplicationInventoryResultDTO();
        inventoryResult2.setProductId("prod-002");
        inventoryResult2.setAvailable(true); // This item is available but not enough
        inventoryResult2.setAvailableQty(5);
        
        ApplicationInventoryCheckResponseDTO inventoryResponse = new ApplicationInventoryCheckResponseDTO();
        inventoryResponse.setResults(List.of(inventoryResult1, inventoryResult2));
        inventoryResponse.setAllAvailable(false); // Overall availability is false
        
        when(inventoryOutputPort.checkAvailability(any(ApplicationInventoryCheckRequestDTO.class)))
            .thenReturn(inventoryResponse);
        
        System.out.println("Inventory unavailable mock responses configured successfully");
    }
    
    private SharedCartCheckedOutEventDTO createValidCartCheckedOutEvent() {
        System.out.println("Creating valid CartCheckedOut event...");
        
        SharedCartCheckedOutEventDTO event = new SharedCartCheckedOutEventDTO();
        event.setCustomerId("cust-12345");
        event.setCartId("cart-" + System.currentTimeMillis());
        event.setTimestamp(Instant.now());
        
        // Create cart items
        SharedCartItemDTO item1 = new SharedCartItemDTO("prod-001", 2, "cart-item-1");
        SharedCartItemDTO item2 = new SharedCartItemDTO("prod-002", 1, "cart-item-2");
        event.setItems(List.of(item1, item2));
        
        // Create billing address
        SharedAddressDTO billingAddress = new SharedAddressDTO();
        billingAddress.setStreet1("123 Main St");
        billingAddress.setStreet2("Apt 4B");
        billingAddress.setCity("New York");
        billingAddress.setState("NY");
        billingAddress.setPostalCode("10001");
        billingAddress.setCountry("US");
        event.setBillingAddress(billingAddress);
        
        // Create shipping address
        SharedAddressDTO shippingAddress = new SharedAddressDTO();
        shippingAddress.setStreet1("456 Oak Ave");
        shippingAddress.setCity("Brooklyn");
        shippingAddress.setState("NY");
        shippingAddress.setPostalCode("11201");
        shippingAddress.setCountry("US");
        event.setShippingAddress(shippingAddress);
        
        // Create payment method
        SharedPaymentMethodDTO paymentMethod = new SharedPaymentMethodDTO();
        paymentMethod.setType("CREDIT_CARD");
        paymentMethod.setProvider("stripe");
        paymentMethod.setDetails(Map.of(
            "cardLast4", "4242",
            "cardBrand", "visa",
            "paymentIntentId", "pi_test_123"
        ));
        event.setPaymentMethod(paymentMethod);
        
        System.out.println("CartCheckedOut event created for cartId: " + event.getCartId());
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
     * Test event listener component to capture Spring Application Events.
     * This is needed because @RecordApplicationEvents doesn't work well with transaction boundaries.
     */
    static class TestEventListener {
        private final List<SharedOrderCreatedEventDTO> orderCreatedEvents = new ArrayList<>();
        private final List<SharedOrderCreationFailedEventDTO> orderCreationFailedEvents = new ArrayList<>();
        private CountDownLatch orderCreatedLatch = new CountDownLatch(1);
        private CountDownLatch orderCreationFailedLatch = new CountDownLatch(1);
        
        @EventListener
        public void handleOrderCreatedEvent(SharedOrderCreatedEventDTO event) {
            System.out.println("TestEventListener captured OrderCreated event: " + event.getOrderId());
            orderCreatedEvents.add(event);
            orderCreatedLatch.countDown();
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
        
        public boolean awaitOrderCreationFailedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderCreationFailedLatch.await(timeout, unit);
        }
        
        public List<SharedOrderCreatedEventDTO> getOrderCreatedEvents() {
            return new ArrayList<>(orderCreatedEvents);
        }
        
        public List<SharedOrderCreationFailedEventDTO> getOrderCreationFailedEvents() {
            return new ArrayList<>(orderCreationFailedEvents);
        }
        
        public void reset() {
            orderCreatedEvents.clear();
            orderCreationFailedEvents.clear();
            orderCreatedLatch = new CountDownLatch(1);
            orderCreationFailedLatch = new CountDownLatch(1);
        }
    }
}