package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderConfirmedEventDTO {
    private String orderId;
    private List<String> reservationIds;
    private String confirmedAt;
}
