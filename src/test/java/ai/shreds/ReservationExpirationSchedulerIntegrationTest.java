package ai.shreds;

import ai.shreds.application.ports.ApplicationReservationInputPort;
import ai.shreds.domain.entities.DomainEntityReservation;
import ai.shreds.domain.entities.DomainEntitySKU;
import ai.shreds.domain.entities.DomainEntityLocation;
import ai.shreds.domain.ports.DomainOutputPortReservationRepository;
import ai.shreds.domain.ports.DomainOutputPortSKURepository;
import ai.shreds.domain.ports.DomainOutputPortLocationRepository;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.dtos.SharedReservationExpiredEventDTO;
import ai.shreds.infrastructure.external_services.InfrastructureKafkaEventPublisher;
import ai.shreds.adapter.primary.AdapterReservationExpirationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles('test')
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(partitions = 1, brokerProperties = {'listeners=PLAINTEXT://localhost:9092', 'port=9092'})
class ReservationExpirationSchedulerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DomainOutputPortReservationRepository reservationRepository;

    @Autowired
    private DomainOutputPortSKURepository skuRepository;

    @Autowired
    private DomainOutputPortLocationRepository locationRepository;

    @SpyBean
    private InfrastructureKafkaEventPublisher kafkaEventPublisher;

    @Autowired
    private AdapterReservationExpirationScheduler scheduler;

    @Autowired
    private ApplicationReservationInputPort reservationService;

    private DomainEntitySKU testSku;
    private DomainEntityLocation testLocation;

    @BeforeEach
    void setUp() {
        // Create test SKU
        testSku = DomainEntitySKU.create(
            new DomainValueProductId('PROD-001'),
            new DomainValueVendorSku('VENDOR-SKU-001')
        );
        skuRepository.save(testSku);

        // Create test location
        testLocation = DomainEntityLocation.create(
            new DomainValueLocationName('Test Warehouse'),
            SharedEnumLocationType.WAREHOUSE,
            new DomainValueAddress('123 Test St', 'Test City', 'TS', '12345', 'US')
        );
        locationRepository.save(testLocation);
    }

    @Test
    @Transactional
    void When_Scheduler_Runs_Then_Expired_Reservations_Are_Marked_As_Expired_And_Events_Published(CapturedOutput output) throws Exception {
        // Given: Create multiple reservations - some expired, some not
        Instant now = Instant.now();
        
        // Create expired reservations
        DomainEntityReservation expiredReservation1 = createReservation(
            testSku.getSkuId(),
            testLocation.getLocationId(),
            10,
            now.minus(Duration.ofMinutes(30)) // Expired 30 minutes ago
        );
        
        DomainEntityReservation expiredReservation2 = createReservation(
            testSku.getSkuId(),
            testLocation.getLocationId(),
            5,
            now.minus(Duration.ofMinutes(15)) // Expired 15 minutes ago
        );
        
        // Create non-expired reservation
        DomainEntityReservation activeReservation = createReservation(
            testSku.getSkuId(),
            testLocation.getLocationId(),
            20,
            now.plus(Duration.ofMinutes(30)) // Expires in 30 minutes
        );

        // Save all reservations
        reservationRepository.save(expiredReservation1);
        reservationRepository.save(expiredReservation2);
        reservationRepository.save(activeReservation);

        // Reset Kafka spy to clear any previous interactions
        reset(kafkaEventPublisher);

        // When: The scheduler runs
        scheduler.processExpiredReservations();

        // Then: Verify expired reservations are processed
        ArgumentCaptor<SharedReservationExpiredEventDTO> eventCaptor = 
            ArgumentCaptor.forClass(SharedReservationExpiredEventDTO.class);
        
        // Verify that publishReservationExpired was called exactly twice (for 2 expired reservations)
        verify(kafkaEventPublisher, times(2)).publishReservationExpired(eventCaptor.capture());
        
        List<SharedReservationExpiredEventDTO> publishedEvents = eventCaptor.getAllValues();
        assertThat(publishedEvents).hasSize(2);
        
        // Verify event contents
        assertThat(publishedEvents)
            .extracting(SharedReservationExpiredEventDTO::getReservationId)
            .containsExactlyInAnyOrder(
                expiredReservation1.getReservationId().getValue().toString(),
                expiredReservation2.getReservationId().getValue().toString()
            );

        // Verify SKUs and quantities in events
        assertThat(publishedEvents)
            .extracting(SharedReservationExpiredEventDTO::getSkuId)
            .containsOnly(testSku.getSkuId().getValue());
        
        assertThat(publishedEvents)
            .extracting(SharedReservationExpiredEventDTO::getQuantity)
            .containsExactlyInAnyOrder(10, 5);

        // Verify reservations are marked as expired in database
        DomainEntityReservation updatedReservation1 = reservationRepository
            .findById(expiredReservation1.getReservationId())
            .orElseThrow();
        assertThat(updatedReservation1.getStatus()).isEqualTo(DomainEnumReservationStatus.EXPIRED);
        
        DomainEntityReservation updatedReservation2 = reservationRepository
            .findById(expiredReservation2.getReservationId())
            .orElseThrow();
        assertThat(updatedReservation2.getStatus()).isEqualTo(DomainEnumReservationStatus.EXPIRED);
        
        // Verify active reservation remains PENDING
        DomainEntityReservation updatedActiveReservation = reservationRepository
            .findById(activeReservation.getReservationId())
            .orElseThrow();
        assertThat(updatedActiveReservation.getStatus()).isEqualTo(DomainEnumReservationStatus.PENDING);

        // Verify logs
        assertThat(output).contains('Starting scheduled reservation expiration processing');
        assertThat(output).contains('Processing expired reservations with batch size:');
        assertThat(output).contains('Reservation ' + expiredReservation1.getReservationId().getValue() + ' expired and inventory released');
        assertThat(output).contains('Reservation ' + expiredReservation2.getReservationId().getValue() + ' expired and inventory released');
        assertThat(output).contains('Completed processing 2 expired reservations');
    }

    private DomainEntityReservation createReservation(
            DomainValueSkuId skuId,
            DomainValueLocationId locationId,
            int quantity,
            Instant expiresAt) {
        return DomainEntityReservation.create(
            skuId,
            locationId,
            new DomainValueQuantity(quantity, DomainEnumQuantityUnit.UNIT),
            expiresAt,
            'Test reservation'
        );
    }
}