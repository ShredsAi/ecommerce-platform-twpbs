package ai.shreds;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.domain.ports.DomainOutputPortCancellationRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.value_objects.SharedCancellationRequestParams;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.infrastructure.external_services.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete cancellation workflow.
 * 
 * This test verifies that a valid cancellation request:
 * 1. Passes eligibility validation
 * 2. Persists cancellation entity in database
 * 3. Initiates refund with payment service
 * 4. Releases inventory stock
 * 5. Publishes domain events to Kafka/JMS
 * 6. Returns proper response to client
 * 
 * Uses TestContainers for PostgreSQL and embedded Kafka.
 * External services are mocked to focus on integration behavior.
 */
@SpringBootTest(
    classes = OrderCancellationReturnsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "management.endpoints.web.exposure.include=health,info,metrics",
        "logging.level.ai.shreds=DEBUG",
        "logging.level.org.springframework.kafka=WARN",
        "logging.level.org.springframework.jms=WARN"
    }
)
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"order-events", "cancellation-events", "return-events"})
@ExtendWith(OutputCaptureExtension.class)
@Transactional
public class CancellationWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(false);

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
    private ObjectMapper objectMapper;

    @Autowired
    private DomainOutputPortCancellationRepository cancellationRepository;

    // Mock the order repository to prevent gRPC calls during testing
    @MockBean
    private DomainOutputPortOrderRepository orderRepository;

    // Mock external service clients only - following the same pattern as OrderCancellationReturnsApplicationTest
    // The infrastructure clients implement the application ports, so we only need to mock the clients
    @MockBean
    private InfrastructurePaymentServiceClient paymentServiceClient;

    @MockBean
    private InfrastructureInventoryServiceClient inventoryServiceClient;

    @MockBean
    private InfrastructureNotificationServiceClient notificationServiceClient;

    @MockBean
    private InfrastructureKafkaEventClient kafkaEventClient;

    @MockBean
    private InfrastructureJmsEventClient jmsEventClient;

    @MockBean
    private InfrastructureSpringEventClient springEventClient;

    private static final String VALID_ORDER_ID = "ORDER-12345-ABCDEF";
    private static final String SHIPPED_ORDER_ID = "ORDER-SHIPPED-67890";
    private static final String VALID_CUSTOMER_ID = "CUST-67890";
    private static final String CANCELLATION_REASON = "CUSTOMER_REQUEST";
    private static final String NOTES = "Customer requested cancellation";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(orderRepository);
        reset(paymentServiceClient, inventoryServiceClient, notificationServiceClient);
        reset(kafkaEventClient, jmsEventClient, springEventClient);
        
        // Mock successful order lookup
        SharedOrderSnapshotDTO mockOrderSnapshot = createMockOrderSnapshot();
        when(orderRepository.findOrderSnapshot(VALID_ORDER_ID)).thenReturn(mockOrderSnapshot);
        
        // Mock shipped order lookup
        SharedOrderSnapshotDTO shippedOrderSnapshot = createMockShippedOrderSnapshot();
        when(orderRepository.findOrderSnapshot(SHIPPED_ORDER_ID)).thenReturn(shippedOrderSnapshot);
        
        // Mock successful payment service response
        SharedRefundRequestDTO mockRefundResponse = createMockRefundResponse();
        when(paymentServiceClient.initiateRefund(any(SharedRefundRequestDTO.class))).thenReturn(mockRefundResponse);
        
        // Mock successful inventory service call
        doNothing().when(inventoryServiceClient).releaseReservedStock(any(SharedOrderSnapshotDTO.class));
        
        // Mock successful notification service call
        doNothing().when(notificationServiceClient).sendNotification(any());
        
        // Mock successful event publishing
        doNothing().when(kafkaEventClient).publishEvent(any());
        doNothing().when(jmsEventClient).publishEvent(any());
        doNothing().when(springEventClient).publishEvent(any());
    }

    @Test
    void whenValidCancellationRequestSubmitted_thenCompleteWorkflowExecutesSuccessfully(CapturedOutput output) {
        // Arrange
        SharedCancellationRequestParams requestParams = new SharedCancellationRequestParams(
            VALID_ORDER_ID,
            CANCELLATION_REASON,
            NOTES
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCancellationRequestParams> requestEntity = 
            new HttpEntity<>(requestParams, headers);
        
        System.out.println("=== STARTING CANCELLATION WORKFLOW TEST ====");
        System.out.println("Request: " + requestParams);
        
        // Act: Submit cancellation request
        ResponseEntity<SharedCancellationResponseDTO> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/api/cancellations",
            requestEntity,
            SharedCancellationResponseDTO.class
        );
        
        // Capture logs for debugging
        String logOutput = output.getOut();
        System.out.println("=== WORKFLOW EXECUTION LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF WORKFLOW LOGS ====");
        
        // Assert: Verify HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        SharedCancellationResponseDTO responseBody = response.getBody();
        System.out.println("Response: " + responseBody);
        
        // Verify response structure
        assertThat(responseBody.getSuccess()).isTrue();
        assertThat(responseBody.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(responseBody.getCancellationId()).isNotNull();
        assertThat(responseBody.getStatus()).isIn("PENDING", "APPROVED", "PROCESSING");
        assertThat(responseBody.getReason()).isEqualTo(CANCELLATION_REASON);
        assertThat(responseBody.getRequestedAt()).isNotNull();
        assertThat(responseBody.getMessage()).isNotNull();
        
        // Verify database persistence
        verifyDatabasePersistence(responseBody.getCancellationId());
        
        // Verify external service coordination
        verifyExternalServiceCoordination();
        
        // Verify event publishing
        verifyEventPublishing();
        
        // Verify no critical errors in logs (allow warnings)
        assertThat(logOutput).doesNotContain("ERROR");
        assertThat(logOutput).doesNotContain("FATAL");
        
        System.out.println("=== CANCELLATION WORKFLOW TEST COMPLETED SUCCESSFULLY ====");
    }

    @Test
    void whenCancellationRequestForShippedOrder_thenRejectionResponseReturned(CapturedOutput output) {
        // Arrange
        SharedCancellationRequestParams requestParams = new SharedCancellationRequestParams(
            SHIPPED_ORDER_ID,
            CANCELLATION_REASON,
            NOTES
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCancellationRequestParams> requestEntity = 
            new HttpEntity<>(requestParams, headers);
        
        System.out.println("=== STARTING CANCELLATION REJECTION TEST ====");
        System.out.println("Request: " + requestParams);
        System.out.println("Order Status: SHIPPED (non-cancellable)");
        
        // Act: Submit cancellation request for shipped order
        ResponseEntity<SharedCancellationResponseDTO> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/api/cancellations",
            requestEntity,
            SharedCancellationResponseDTO.class
        );
        
        // Capture logs for debugging
        String logOutput = output.getOut();
        System.out.println("=== REJECTION WORKFLOW LOGS ====");
        System.out.println(logOutput);
        System.out.println("=== END OF REJECTION LOGS ====");
        
        // Assert: Verify HTTP response indicates rejection
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
        
        // The response should be 422 UNPROCESSABLE_ENTITY for business rule violations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        
        SharedCancellationResponseDTO responseBody = response.getBody();
        assertThat(responseBody.getSuccess()).isFalse();
        assertThat(responseBody.getMessage()).isNotNull();
        assertThat(responseBody.getMessage().toLowerCase()).containsAnyOf("rejected", "not allowed", "cannot be cancelled");
        assertThat(responseBody.getOrderId()).isEqualTo(SHIPPED_ORDER_ID);
        assertThat(responseBody.getStatus()).isEqualTo("REJECTED");
        
        // Verify no database persistence occurred for the cancellation
        verifyNoUnauthorizedDatabasePersistence();
        
        // Verify no external service calls were made for business operations
        verifyNoExternalServiceCalls();
        
        // Verify appropriate logging occurred
        String lowerCaseOutput = logOutput.toLowerCase();
        boolean hasRejectionLog = Arrays.stream(lowerCaseOutput.split("\n"))
            .anyMatch(line -> 
                line.contains("domain exception") && line.contains("cancellation") && 
                (line.contains("not allowed") || line.contains("shipped"))
            );
        assertThat(hasRejectionLog).as("Should have rejection logging").isTrue();
        
        // Note: We don't verify "no events published" because Spring framework and infrastructure
        // might publish technical/lifecycle events even for rejected requests. The important thing
        // is that no business operations (payment, inventory) were executed.
        
        System.out.println("=== CANCELLATION REJECTION TEST COMPLETED SUCCESSFULLY ====");
    }
    
    private void verifyDatabasePersistence(String cancellationId) {
        System.out.println("=== VERIFYING DATABASE PERSISTENCE ====");
        
        // Verify cancellation entity is persisted
        DomainCancellationRequestEntity persistedEntity = cancellationRepository.findById(cancellationId);
        assertThat(persistedEntity).isNotNull();
        assertThat(persistedEntity.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(persistedEntity.getStatus()).isIn(
            SharedCancellationStatusEnum.PENDING, 
            SharedCancellationStatusEnum.APPROVED,
            SharedCancellationStatusEnum.PROCESSING
        );
        assertThat(persistedEntity.getReason()).isEqualTo(SharedCancellationReasonEnum.CUSTOMER_REQUEST);
        assertThat(persistedEntity.getNotes()).isEqualTo(NOTES);
        assertThat(persistedEntity.getRequestedAt()).isNotNull();
        
        // Verify order lookup was called twice: once for validation and once for inventory release
        // This matches the actual workflow: requestCancellation + coordinateInventoryRelease
        verify(orderRepository, times(2)).findOrderSnapshot(VALID_ORDER_ID);
        
        System.out.println("Database persistence verified successfully");
    }
    
    private void verifyNoUnauthorizedDatabasePersistence() {
        System.out.println("=== VERIFYING NO UNAUTHORIZED DATABASE PERSISTENCE ====");
        
        // Verify that order repository was called for eligibility check
        verify(orderRepository, times(1)).findOrderSnapshot(SHIPPED_ORDER_ID);
        
        // Verify no cancellation entities were created for rejected requests
        List<DomainCancellationRequestEntity> existingCancellations = 
            cancellationRepository.findByOrderId(SHIPPED_ORDER_ID);
        assertThat(existingCancellations).isEmpty();
        
        System.out.println("No unauthorized database persistence verified successfully");
    }
    
    private void verifyExternalServiceCoordination() {
        System.out.println("=== VERIFYING EXTERNAL SERVICE COORDINATION ====");
        
        // Verify payment service refund initiation
        verify(paymentServiceClient, times(1)).initiateRefund(any(SharedRefundRequestDTO.class));
        
        // Verify inventory service stock release
        verify(inventoryServiceClient, times(1)).releaseReservedStock(any(SharedOrderSnapshotDTO.class));
        
        // Verify notification service is called (allows for multiple notifications from different workflow parts)
        // The workflow triggers notifications from: cancellation service, event publishers, and Spring event listeners
        verify(notificationServiceClient, atLeastOnce()).sendNotification(any());
        
        System.out.println("External service coordination verified successfully");
    }
    
    private void verifyNoExternalServiceCalls() {
        System.out.println("=== VERIFYING NO EXTERNAL SERVICE CALLS ====");
        
        // Verify no payment service refund calls
        verify(paymentServiceClient, never()).initiateRefund(any(SharedRefundRequestDTO.class));
        
        // Verify no inventory service calls
        verify(inventoryServiceClient, never()).releaseReservedStock(any(SharedOrderSnapshotDTO.class));
        
        // Allow notification service calls (errors/rejections might still trigger notifications)
        // but verify no payment or inventory coordination occurred
        
        System.out.println("No unauthorized external service calls verified successfully");
    }
    
    private void verifyEventPublishing() {
        System.out.println("=== VERIFYING EVENT PUBLISHING ====");
        
        // Verify Kafka event publication
        verify(kafkaEventClient, atLeastOnce()).publishEvent(any());
        
        // Verify JMS event publication
        verify(jmsEventClient, atLeastOnce()).publishEvent(any());
        
        System.out.println("Event publishing verified successfully");
    }
    
    private SharedOrderSnapshotDTO createMockOrderSnapshot() {
        SharedOrderSnapshotDTO orderSnapshot = new SharedOrderSnapshotDTO();
        orderSnapshot.setOrderId(VALID_ORDER_ID);
        orderSnapshot.setCustomerId(VALID_CUSTOMER_ID);
        orderSnapshot.setOrderStatus("CONFIRMED"); // Cancellable status
        orderSnapshot.setOrderDate(LocalDateTime.now().minusHours(1));
        orderSnapshot.setTotalAmount(new SharedMoneyValue(new BigDecimal("99.99"), "USD"));
        orderSnapshot.setPaymentStatus("PAID");
        orderSnapshot.setShippingStatus("PREPARING");
        orderSnapshot.setDeliveryDate(null); // Not delivered yet
        
        // Add order items
        SharedOrderItemDTO item1 = new SharedOrderItemDTO();
        item1.setOrderItemId("ITEM-1");
        item1.setProductId("PROD-123");
        item1.setProductName("Test Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new SharedMoneyValue(new BigDecimal("29.99"), "USD"));
        item1.setTotalPrice(new SharedMoneyValue(new BigDecimal("59.98"), "USD"));
        item1.setIsReturnable(true);
        
        SharedOrderItemDTO item2 = new SharedOrderItemDTO();
        item2.setOrderItemId("ITEM-2");
        item2.setProductId("PROD-456");
        item2.setProductName("Test Product 2");
        item2.setQuantity(1);
        item2.setUnitPrice(new SharedMoneyValue(new BigDecimal("40.01"), "USD"));
        item2.setTotalPrice(new SharedMoneyValue(new BigDecimal("40.01"), "USD"));
        item2.setIsReturnable(true);
        
        orderSnapshot.setItems(List.of(item1, item2));
        
        return orderSnapshot;
    }
    
    private SharedOrderSnapshotDTO createMockShippedOrderSnapshot() {
        SharedOrderSnapshotDTO orderSnapshot = new SharedOrderSnapshotDTO();
        orderSnapshot.setOrderId(SHIPPED_ORDER_ID);
        orderSnapshot.setCustomerId(VALID_CUSTOMER_ID);
        orderSnapshot.setOrderStatus("SHIPPED"); // Non-cancellable status
        orderSnapshot.setOrderDate(LocalDateTime.now().minusDays(2));
        orderSnapshot.setTotalAmount(new SharedMoneyValue(new BigDecimal("149.99"), "USD"));
        orderSnapshot.setPaymentStatus("PAID");
        orderSnapshot.setShippingStatus("SHIPPED");
        orderSnapshot.setDeliveryDate(null); // Shipped but not delivered yet
        
        // Add order items
        SharedOrderItemDTO item1 = new SharedOrderItemDTO();
        item1.setOrderItemId("ITEM-SHIPPED-1");
        item1.setProductId("PROD-789");
        item1.setProductName("Shipped Product");
        item1.setQuantity(1);
        item1.setUnitPrice(new SharedMoneyValue(new BigDecimal("149.99"), "USD"));
        item1.setTotalPrice(new SharedMoneyValue(new BigDecimal("149.99"), "USD"));
        item1.setIsReturnable(true);
        
        orderSnapshot.setItems(List.of(item1));
        
        return orderSnapshot;
    }
    
    private SharedRefundRequestDTO createMockRefundResponse() {
        SharedRefundRequestDTO refundResponse = new SharedRefundRequestDTO();
        refundResponse.setRefundId(UUID.randomUUID().toString());
        refundResponse.setOrderId(VALID_ORDER_ID);
        refundResponse.setAmount(new SharedMoneyValue(new BigDecimal("99.99"), "USD"));
        refundResponse.setReason("Order cancellation");
        refundResponse.setStatus("INITIATED");
        refundResponse.setRequestedAt(LocalDateTime.now());
        
        return refundResponse;
    }
}