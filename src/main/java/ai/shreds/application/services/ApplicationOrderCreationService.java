package ai.shreds.application.services;

import ai.shreds.application.dtos.*;
import ai.shreds.application.exceptions.ApplicationOrderCreationException;
import ai.shreds.application.ports.*;
import ai.shreds.domain.ports.DomainInputPortCreateOrder;
import ai.shreds.domain.services.DomainServiceValidation;
import ai.shreds.domain.value_objects.DomainOrderAggregate;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;
import ai.shreds.shared.enums.SharedErrorTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class ApplicationOrderCreationService implements ApplicationCreateOrderInputPort {

    private final ApplicationInventoryOutputPort inventoryPort;
    private final ApplicationPricingOutputPort pricingPort;
    private final ApplicationOrderRepositoryOutputPort orderRepositoryPort;
    private final ApplicationEventPublisherOutputPort eventPublisherPort;
    private final ApplicationIdempotencyService idempotencyService;
    private final DomainInputPortCreateOrder orderCreationUseCase;
    private final DomainServiceValidation validationService;

    public ApplicationOrderCreationService(
            ApplicationInventoryOutputPort inventoryPort,
            ApplicationPricingOutputPort pricingPort,
            ApplicationOrderRepositoryOutputPort orderRepositoryPort,
            ApplicationEventPublisherOutputPort eventPublisherPort,
            ApplicationIdempotencyService idempotencyService,
            DomainInputPortCreateOrder orderCreationUseCase,
            DomainServiceValidation validationService) {
        this.inventoryPort = inventoryPort;
        this.pricingPort = pricingPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.idempotencyService = idempotencyService;
        this.orderCreationUseCase = orderCreationUseCase;
        this.validationService = validationService;
    }

    @Override
    @Transactional
    public ApplicationOrderCreationResponseDTO execute(ApplicationOrderCreationRequestDTO request) {
        try {
            if (idempotencyService.isDuplicate(request.getCartId())) {
                DomainOrderAggregate existing = orderRepositoryPort.findByCartId(request.getCartId())
                        .orElseThrow(() -> new ApplicationOrderCreationException(
                                "Duplicate request but no existing order found", null,
                                SharedErrorTypeEnum.PERSISTENCE_ERROR, request.getCartId()));
                return ApplicationOrderCreationResponseDTO.fromDomainOrder(existing);
            }
            idempotencyService.registerProcessing(request.getCartId());

            // Inventory check
            ApplicationInventoryCheckRequestDTO invReq = new ApplicationInventoryCheckRequestDTO(request.getItems());
            ApplicationInventoryCheckResponseDTO invRes = inventoryPort.checkAvailability(invReq);
            if (!invRes.isAllAvailable()) {
                throw new ApplicationOrderCreationException(
                        "Inventory unavailable", null,
                        SharedErrorTypeEnum.INVENTORY_UNAVAILABLE, request.getCartId());
            }

            // Pricing
            ApplicationPricingRequestDTO priceReq = new ApplicationPricingRequestDTO(
                    request.getCustomerId(),
                    request.getItems().stream()
                            .map(i -> new ApplicationPricingItemDTO(i.getProductId(), i.getQuantity()))
                            .collect(Collectors.toList()),
                    request.getBillingAddress(),
                    request.getShippingAddress(),
                    Collections.emptyList()
            );
            ApplicationPricingResponseDTO priceRes = pricingPort.calculatePricing(priceReq);

            // Validation
            validationService.validateCustomer(request.getCustomerId());
            validationService.validateAddresses(
                    request.getBillingAddress().toDomainValue(),
                    request.getShippingAddress().toDomainValue()
            );
            validationService.validateBusinessRules(
                    request.getItems().size(),
                    priceRes.getTotal().toDomainValue()
            );

            // Domain use case
            DomainOrderAggregate aggregate = orderCreationUseCase.execute(
                    request.getCartId(),
                    request.getCustomerId(),
                    request.getItems(),
                    request.getBillingAddress(),
                    request.getShippingAddress(),
                    priceRes,
                    request.getPaymentMethod()
            );

            DomainOrderAggregate saved = orderRepositoryPort.save(aggregate);

            // Publish success event
            eventPublisherPort.publishOrderCreated(saved.toDTO());

            return ApplicationOrderCreationResponseDTO.fromDomainOrder(saved);
        } catch (ApplicationOrderCreationException ex) {
            publishFailure(request, ex);
            throw ex;
        } catch (Exception ex) {
            ApplicationOrderCreationException appEx = new ApplicationOrderCreationException(
                    ex.getMessage(), ex,
                    SharedErrorTypeEnum.PERSISTENCE_ERROR, request.getCartId()
            );
            publishFailure(request, appEx);
            throw appEx;
        }
    }

    private void publishFailure(ApplicationOrderCreationRequestDTO request,
                                ApplicationOrderCreationException ex) {
        SharedOrderCreationFailedEventDTO event = new SharedOrderCreationFailedEventDTO();
        event.setCartId(request.getCartId());
        event.setCustomerId(request.getCustomerId());
        event.setErrorType(ex.getErrorType());
        event.setErrorMessage(ex.getMessage());
        event.setFailureReason(ex.getErrorType().name());
        event.setTimestamp(Instant.now());
        eventPublisherPort.publishOrderCreationFailed(event);
    }
}
