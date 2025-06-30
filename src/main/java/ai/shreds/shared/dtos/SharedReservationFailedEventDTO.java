package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reservation failed event published to Kafka.
 * Sent when reservation creation for any cart item fails.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedReservationFailedEventDTO {
    private String cartId;
    private String error;
    private List<SharedFailedItemDTO> failedItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedFailedItemDTO {
        private String skuId;
        private String locationId;
        private int requestedQuantity;
        private int availableQuantity;
    }
}
