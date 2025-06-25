package ai.shreds.infrastructure.external_services;

import ai.shreds.application.dtos.ApplicationInventoryCheckRequestDTO;
import ai.shreds.application.dtos.ApplicationInventoryCheckResponseDTO;
import ai.shreds.application.dtos.ApplicationPricingRequestDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.application.dtos.ApplicationItemPricingDTO;
import ai.shreds.application.dtos.ApplicationInventoryResultDTO;
import ai.shreds.shared.grpc.SharedGrpcValidateStockAvailabilityRequest;
import ai.shreds.shared.grpc.SharedGrpcValidateStockAvailabilityResponse;
import ai.shreds.shared.grpc.SharedGrpcCalculateOrderPricingRequest;
import ai.shreds.shared.grpc.SharedGrpcCalculateOrderPricingResponse;
import ai.shreds.shared.dtos.SharedAddressDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.grpc.ItemRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
import ai.shreds.grpc.pricing.v1.Address;
import ai.shreds.grpc.pricing.v1.Money;
import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

/**
 * Mapper between application DTOs and shared gRPC DTOs for inventory and pricing services.
 */
@Component
public class InfrastructureGrpcMapper {

    public SharedGrpcValidateStockAvailabilityRequest toGrpcInventoryRequest(ApplicationInventoryCheckRequestDTO dto) {
        List<ItemRequest> items = dto.getItems().stream()
            .map(i -> new ItemRequest(i.getProductId(), i.getQuantity()))
            .collect(Collectors.toList());
        return new SharedGrpcValidateStockAvailabilityRequest(items);
    }

    public ApplicationInventoryCheckResponseDTO fromGrpcInventoryResponse(SharedGrpcValidateStockAvailabilityResponse response) {
        List<ApplicationInventoryResultDTO> results = response.getResults().stream()
            .map(r -> new ApplicationInventoryResultDTO(r.getProductId(), r.getAvailable(), r.getAvailableQty()))
            .collect(Collectors.toList());
        boolean allAvailable = results.stream().allMatch(ApplicationInventoryResultDTO::isAvailable);
        return new ApplicationInventoryCheckResponseDTO(results, allAvailable);
    }

    public SharedGrpcCalculateOrderPricingRequest toGrpcPricingRequest(ApplicationPricingRequestDTO dto) {
        List<SharedGrpcCalculateOrderPricingRequest.PricingItem> items = dto.getItems().stream()
            .map(i -> new SharedGrpcCalculateOrderPricingRequest.PricingItem(i.getProductId(), i.getQuantity()))
            .collect(Collectors.toList());
        Address billing = mapAddress(requireNonNull(dto.getBillingAddress()));
        Address shipping = mapAddress(requireNonNull(dto.getShippingAddress()));
        return new SharedGrpcCalculateOrderPricingRequest(
            dto.getCustomerId(),
            items,
            billing,
            shipping,
            dto.getPromotions()
        );
    }

    public ApplicationPricingResponseDTO fromGrpcPricingResponse(SharedGrpcCalculateOrderPricingResponse response) {
        SharedMoneyDTO subtotal = new SharedMoneyDTO(
            BigDecimal.valueOf(response.getSubtotal().getAmount()),
            response.getSubtotal().getCurrency()
        );
        SharedMoneyDTO tax = new SharedMoneyDTO(
            BigDecimal.valueOf(response.getTax().getAmount()),
            response.getTax().getCurrency()
        );
        SharedMoneyDTO discounts = new SharedMoneyDTO(
            BigDecimal.valueOf(response.getDiscounts().getAmount()),
            response.getDiscounts().getCurrency()
        );
        SharedMoneyDTO total = new SharedMoneyDTO(
            BigDecimal.valueOf(response.getTotal().getAmount()),
            response.getTotal().getCurrency()
        );

        List<ApplicationItemPricingDTO> breakdown = response.getItemBreakdown().stream()
            .map(ip -> new ApplicationItemPricingDTO(
                ip.getProductId(),
                ip.getUnitPrice(),
                ip.getTotalPrice()
            ))
            .collect(Collectors.toList());
        return new ApplicationPricingResponseDTO(subtotal, tax, discounts, total, breakdown);
    }

    private Address mapAddress(SharedAddressDTO address) {
        return Address.newBuilder()
            .setStreet1(address.getStreet1())
            .setStreet2(address.getStreet2() != null ? address.getStreet2() : "")
            .setCity(address.getCity())
            .setState(address.getState() != null ? address.getState() : "")
            .setPostalCode(address.getPostalCode())
            .setCountry(address.getCountry())
            .build();
    }

    private Money mapMoney(SharedMoneyDTO money) {
        return Money.newBuilder()
            .setAmount(money.getAmount().longValue())
            .setCurrency(money.getCurrency())
            .build();
    }
}