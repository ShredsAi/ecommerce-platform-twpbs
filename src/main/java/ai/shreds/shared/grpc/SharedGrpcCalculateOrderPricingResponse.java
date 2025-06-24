package ai.shreds.shared.grpc;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import ai.shreds.grpc.pricing.v1.CalculateOrderPricingResponse;
import ai.shreds.grpc.pricing.v1.Money;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedGrpcCalculateOrderPricingResponse {
    private Money subtotal;
    private Money tax;
    private Money discounts;
    private Money total;
    private List<ItemPricing> itemBreakdown;

    public static SharedGrpcCalculateOrderPricingResponse fromProto(CalculateOrderPricingResponse proto) {
        List<ItemPricing> breakdown = new ArrayList<>();
        if (proto.getItemBreakdownCount() > 0) {
            for (ai.shreds.grpc.pricing.v1.ItemPricing ip : proto.getItemBreakdownList()) {
                // Extract minor unit amount and convert to BigDecimal
                BigDecimal unit = BigDecimal.valueOf(ip.getUnitPrice().getAmount());
                BigDecimal totalPrice = BigDecimal.valueOf(ip.getTotalPrice().getAmount());
                breakdown.add(new ItemPricing(ip.getProductId(), unit, totalPrice));
            }
        }
        return new SharedGrpcCalculateOrderPricingResponse(
            proto.getSubtotal(),
            proto.getTax(),
            proto.getDiscounts(),
            proto.getTotal(),
            breakdown
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPricing {
        private String productId;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}