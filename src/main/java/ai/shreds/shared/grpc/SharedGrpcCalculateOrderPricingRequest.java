package ai.shreds.shared.grpc;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import ai.shreds.grpc.pricing.v1.CalculateOrderPricingRequest;
import ai.shreds.grpc.pricing.v1.Address;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedGrpcCalculateOrderPricingRequest {
    private String customerId;
    private List<PricingItem> items;
    private Address billingAddress;
    private Address shippingAddress;
    private List<String> promotions;

    public CalculateOrderPricingRequest toProto() {
        CalculateOrderPricingRequest.Builder builder = CalculateOrderPricingRequest.newBuilder()
            .setCustomerId(customerId == null ? "" : customerId);
        if (items != null) {
            for (PricingItem item : items) {
                builder.addItems(ai.shreds.grpc.pricing.v1.PricingItem.newBuilder()
                    .setProductId(item.getProductId() == null ? "" : item.getProductId())
                    .setQuantity(item.getQuantity() == null ? 0 : item.getQuantity())
                    .build());
            }
        }
        if (billingAddress != null) {
            builder.setBillingAddress(billingAddress);
        }
        if (shippingAddress != null) {
            builder.setShippingAddress(shippingAddress);
        }
        if (promotions != null) {
            builder.addAllPromotions(promotions);
        }
        return builder.build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingItem {
        private String productId;
        private Integer quantity;
    }
}