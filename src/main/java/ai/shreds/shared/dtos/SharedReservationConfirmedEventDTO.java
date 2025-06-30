package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedReservationConfirmedEventDTO {
    private String reservationId;
    private String orderId;
    private String skuId;
    private String locationId;
    private String status;
    private String confirmedAt;
}
