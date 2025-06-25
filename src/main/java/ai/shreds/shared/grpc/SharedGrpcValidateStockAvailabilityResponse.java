package ai.shreds.shared.grpc;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import ai.shreds.grpc.inventory.v1.ValidateStockAvailabilityResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedGrpcValidateStockAvailabilityResponse {
    private List<ItemResult> results;

    public static SharedGrpcValidateStockAvailabilityResponse fromProto(ValidateStockAvailabilityResponse proto) {
        List<ItemResult> list = new ArrayList<>();
        if (proto.getResultsCount() > 0) {
            // Iterate over top-level ItemResult messages
            for (ai.shreds.grpc.inventory.v1.ItemResult item : proto.getResultsList()) {
                list.add(new ItemResult(
                    item.getProductId(),
                    item.getAvailable(),
                    item.getAvailableQty()
                ));
            }
        }
        return new SharedGrpcValidateStockAvailabilityResponse(list);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private String productId;
        private Boolean available;
        private Integer availableQty;
    }
}