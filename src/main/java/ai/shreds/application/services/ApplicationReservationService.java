package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationReservationInputPort;
import ai.shreds.application.ports.ApplicationStockValidationInputPort;
import ai.shreds.domain.entities.DomainEntityReservation;
import ai.shreds.domain.exceptions.DomainExceptionEntityNotFound;
import ai.shreds.domain.ports.DomainOutputPortReservationRepository;
import ai.shreds.domain.value_objects.DomainEnumQuantityUnit;
import ai.shreds.domain.value_objects.DomainValueLocationId;
import ai.shreds.domain.value_objects.DomainValueQuantity;
import ai.shreds.domain.value_objects.DomainValueReservationId;
import ai.shreds.domain.value_objects.DomainValueSkuId;
import ai.shreds.shared.dtos.*;
import ai.shreds.shared.value_objects.SharedStockValidationRequestEvent;
import ai.shreds.shared.value_objects.SharedStockValidationResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationReservationService implements ApplicationReservationInputPort {

    private final ApplicationStockValidationInputPort stockValidationPort;
    private final DomainOutputPortReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationEventPublisherOutputPort kafkaEventPublisher;

    private final ConcurrentHashMap<UUID, CheckoutState> checkoutStates = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void processCartCheckout(SharedCartCheckoutEvent event) {
        log.info("Processing cart checkout for cartId: {}", event.getCartId());
        List<CartItemDTO> items = event.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("Cart {} has no items to reserve.", event.getCartId());
            return;
        }

        CheckoutState state = new CheckoutState(items.size());
        checkoutStates.put(event.getCartId(), state);

        items.forEach(item -> {
            SharedStockValidationRequestEvent validationRequest = new SharedStockValidationRequestEvent(
                    item.getSkuId(), item.getLocationId(), item.getQuantity());
            eventPublisher.publishEvent(validationRequest);
        });

        try {
            boolean allValidated = state.getLatch().await(10, TimeUnit.SECONDS);
            if (allValidated && state.isAllAvailable()) {
                createReservations(event);
                log.info("Successfully created reservations for cartId: {}", event.getCartId());
                
                // Publish success event to Kafka
                SharedReservationCreatedEventDTO createdEvent = new SharedReservationCreatedEventDTO(
                    state.getReservationId().toString(),
                    event.getCartId().toString(),
                    "PENDING",
                    Instant.now().plus(Duration.ofMinutes(15)).toString()
                );
                kafkaEventPublisher.publishReservationCreated(createdEvent);
            } else {
                log.warn("Failed to reserve items for cartId: {}. Not all items were available.", event.getCartId());
                
                // Publish failure event to Kafka
                List<SharedReservationFailedEventDTO.SharedFailedItemDTO> failedItems = new ArrayList<>();
                state.getValidationResults().forEach((key, result) -> {
                    if (!result.getIsAvailable()) {
                        failedItems.add(new SharedReservationFailedEventDTO.SharedFailedItemDTO(
                            result.getSkuId(),
                            result.getLocationId(),
                            result.getRequestedQuantity().intValue(),
                            result.getAvailableQuantity().intValue()
                        ));
                    }
                });
                
                SharedReservationFailedEventDTO failedEvent = new SharedReservationFailedEventDTO(
                    event.getCartId().toString(),
                    "INSUFFICIENT_STOCK",
                    failedItems
                );
                kafkaEventPublisher.publishReservationFailed(failedEvent);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for stock validation for cartId: {}", event.getCartId(), e);
        } finally {
            checkoutStates.remove(event.getCartId());
        }
    }

    @Override
    @Transactional
    public void processOrderConfirmed(SharedOrderConfirmedEventDTO event) {
        log.info("Processing order confirmation for orderId: {} with reservations: {}", 
            event.getOrderId(), event.getReservationIds());
        
        for (String reservationIdStr : event.getReservationIds()) {
            try {
                DomainValueReservationId reservationId = new DomainValueReservationId(UUID.fromString(reservationIdStr));
                DomainEntityReservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new DomainExceptionEntityNotFound("Reservation", reservationIdStr));
                
                // Confirm the reservation
                reservation.confirm();
                reservationRepository.save(reservation);
                
                log.info("Reservation {} confirmed for order {}", reservationIdStr, event.getOrderId());
                
                // Publish reservation confirmed event
                SharedReservationConfirmedEventDTO confirmedEvent = new SharedReservationConfirmedEventDTO(
                    reservationIdStr,
                    event.getOrderId(),
                    reservation.getSkuId().getValue(),
                    reservation.getLocationId().getValue(),
                    "CONFIRMED",
                    event.getConfirmedAt()
                );
                kafkaEventPublisher.publishReservationConfirmed(confirmedEvent);
                
            } catch (Exception e) {
                log.error("Failed to confirm reservation {} for order {}", reservationIdStr, event.getOrderId(), e);
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void processExpiredReservations(int batchSize) {
        log.info("Processing expired reservations with batch size: {}", batchSize);
        List<DomainEntityReservation> expiredReservations = reservationRepository.findExpiredReservations(batchSize);
        
        for (DomainEntityReservation reservation : expiredReservations) {
            try {
                // Mark reservation as expired
                reservation.expire();
                reservationRepository.save(reservation);
                
                log.info("Reservation {} expired and inventory released", reservation.getReservationId().getValue());
                
                // Publish reservation expired event
                SharedReservationExpiredEventDTO expiredEvent = new SharedReservationExpiredEventDTO(
                    reservation.getReservationId().getValue().toString(),
                    reservation.getSkuId().getValue(),
                    reservation.getQuantity().getValue(),
                    Instant.now().toString()
                );
                kafkaEventPublisher.publishReservationExpired(expiredEvent);
                
            } catch (Exception e) {
                log.error("Failed to process expired reservation {}", reservation.getReservationId().getValue(), e);
                // Continue processing other reservations even if one fails
            }
        }
        
        log.info("Completed processing {} expired reservations", expiredReservations.size());
    }

    @EventListener
    public void handleStockValidationResponse(SharedStockValidationResponseEvent response) {
        checkoutStates.values().forEach(state -> {
            state.addResponse(response);
            state.getLatch().countDown();
        });
    }

    private void createReservations(SharedCartCheckoutEvent event) {
        List<DomainEntityReservation> reservations = event.getItems().stream()
                .map(item -> DomainEntityReservation.create(
                        new DomainValueSkuId(item.getSkuId()),
                        new DomainValueLocationId(item.getLocationId()),
                        new DomainValueQuantity(item.getQuantity(), DomainEnumQuantityUnit.UNIT),
                        Instant.now().plus(Duration.ofMinutes(15)),
                        "Cart checkout: " + event.getCartId()
                ))
                .toList();

        reservations.forEach(reservationRepository::save);
    }

    private static class CheckoutState {
        private final CountDownLatch latch;
        private final ConcurrentHashMap<String, SharedStockValidationResponseEvent> validationResults = new ConcurrentHashMap<>();
        private UUID reservationId;

        public CheckoutState(int itemCount) {
            this.latch = new CountDownLatch(itemCount);
            this.reservationId = UUID.randomUUID();
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public UUID getReservationId() {
            return reservationId;
        }

        public void addResponse(SharedStockValidationResponseEvent response) {
            String key = response.getSkuId() + ":" + response.getLocationId();
            validationResults.put(key, response);
        }

        public ConcurrentHashMap<String, SharedStockValidationResponseEvent> getValidationResults() {
            return validationResults;
        }

        public boolean isAllAvailable() {
            return !validationResults.values().stream()
                .anyMatch(result -> !result.getIsAvailable());
        }
    }
}
