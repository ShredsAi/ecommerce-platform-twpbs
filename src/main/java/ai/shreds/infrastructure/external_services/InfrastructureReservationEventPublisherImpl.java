package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.domain.entities.DomainEntityOutboxEvent;
import ai.shreds.domain.ports.DomainOutputPortOutboxRepository;
import ai.shreds.shared.dtos.SharedReservationConfirmedEventDTO;
import ai.shreds.shared.dtos.SharedReservationCreatedEventDTO;
import ai.shreds.shared.dtos.SharedReservationFailedEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of the ApplicationEventPublisherOutputPort that publishes reservation
 * events to Kafka using the transactional outbox pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InfrastructureReservationEventPublisherImpl implements ApplicationEventPublisherOutputPort {

    private static final String TOPIC_RESERVATION_CREATED = "reservation.created";
    private static final String TOPIC_RESERVATION_FAILED = "reservation.failed";
    private static final String TOPIC_RESERVATION_CONFIRMED = "reservation.confirmed";
    
    private final DomainOutputPortOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishReservationCreated(SharedReservationCreatedEventDTO event) {
        log.info("Publishing reservation created event for reservationId: {}, cartId: {}",
                event.getReservationId(), event.getCartId());
        
        try {
            // Store in outbox first (part of the same transaction as the reservation creation)
            saveToOutbox(UUID.fromString(event.getReservationId()), "ReservationCreated", event);
            
            // Then publish to Kafka - this will be a separate transaction
            // In a real system, a separate process would poll the outbox and publish events
            kafkaTemplate.send(TOPIC_RESERVATION_CREATED, event.getReservationId(), event);
            
            log.debug("Successfully published reservation created event to Kafka for reservationId: {}",
                    event.getReservationId());
        } catch (Exception e) {
            // The outbox ensures the event is still stored, even if Kafka publish fails
            log.error("Error publishing reservation created event to Kafka for reservationId: {}",
                    event.getReservationId(), e);
        }
    }

    @Override
    @Transactional
    public void publishReservationFailed(SharedReservationFailedEventDTO event) {
        log.info("Publishing reservation failed event for cartId: {}, error: {}",
                event.getCartId(), event.getError());
        
        try {
            // Store in outbox first
            // Using cartId as the aggregate ID since there's no reservation ID for failed reservations
            saveToOutbox(UUID.fromString(event.getCartId()), "ReservationFailed", event);
            
            // Then publish to Kafka
            kafkaTemplate.send(TOPIC_RESERVATION_FAILED, event.getCartId(), event);
            
            log.debug("Successfully published reservation failed event to Kafka for cartId: {}",
                    event.getCartId());
        } catch (Exception e) {
            log.error("Error publishing reservation failed event to Kafka for cartId: {}",
                    event.getCartId(), e);
        }
    }

    @Override
    @Transactional
    public void publishReservationConfirmed(SharedReservationConfirmedEventDTO event) {
        log.info("Publishing reservation confirmed event for reservationId: {}, orderId: {}",
                event.getReservationId(), event.getOrderId());
        
        try {
            // Store in outbox first (part of the same transaction as the reservation confirmation)
            saveToOutbox(UUID.fromString(event.getReservationId()), "ReservationConfirmed", event);
            
            // Then publish to Kafka
            kafkaTemplate.send(TOPIC_RESERVATION_CONFIRMED, event.getReservationId(), event);
            
            log.debug("Successfully published reservation confirmed event to Kafka for reservationId: {}",
                    event.getReservationId());
        } catch (Exception e) {
            // The outbox ensures the event is still stored, even if Kafka publish fails
            log.error("Error publishing reservation confirmed event to Kafka for reservationId: {}",
                    event.getReservationId(), e);
        }
    }

    @SneakyThrows
    private void saveToOutbox(UUID aggregateId, String eventType, Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        DomainEntityOutboxEvent outboxEvent = DomainEntityOutboxEvent.create(
            aggregateId,
            "Reservation",
            eventType,
            payloadJson
        );
        
        outboxRepository.save(outboxEvent);
    }
}
