package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedReservationExpiredEventDTO {
    private String reservationId;
    private String skuId;
    private int quantity;
    private String expiredAt;
}