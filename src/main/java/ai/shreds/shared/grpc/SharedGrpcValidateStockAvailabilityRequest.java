package ai.shreds.shared.grpc;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

// Correct import for the protobuf generated class
import ai.shreds.grpc.inventory.v1.ValidateStockAvailabilityRequest;
// ai.shreds.shared.grpc.ItemRequest is implicitly available if it's in the same package or imported.
// The protobuf message ai.shreds.grpc.inventory.v1.ItemRequest will be referenced by its FQN.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedGrpcValidateStockAvailabilityRequest {
    // This 'items' list is of type ai.shreds.shared.grpc.ItemRequest
    private List<ItemRequest> items;

    public ai.shreds.grpc.inventory.v1.ValidateStockAvailabilityRequest toProto() {
        ai.shreds.grpc.inventory.v1.ValidateStockAvailabilityRequest.Builder protoRequestBuilder = 
            ai.shreds.grpc.inventory.v1.ValidateStockAvailabilityRequest.newBuilder();
        
        if (this.items != null) {
            for (ai.shreds.shared.grpc.ItemRequest sharedItemRequest : this.items) {
                if (sharedItemRequest != null) {
                    ai.shreds.grpc.inventory.v1.ItemRequest.Builder protoItemBuilder = 
                        ai.shreds.grpc.inventory.v1.ItemRequest.newBuilder();
                    
                    if (sharedItemRequest.getProductId() != null) {
                        protoItemBuilder.setProductId(sharedItemRequest.getProductId());
                    }
                    // Assuming getQuantity() returns Integer, protobuf expects int32
                    if (sharedItemRequest.getQuantity() != null) {
                         protoItemBuilder.setQuantity(sharedItemRequest.getQuantity());
                    }
                    // The proto ItemRequest has an optional warehouse_id which is not in shared.grpc.ItemRequest.
                    // It will not be set, which is acceptable for an optional field.
                    protoRequestBuilder.addItems(protoItemBuilder.build());
                }
            }
        }
        return protoRequestBuilder.build();
    }
}
