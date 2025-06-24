package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedCartItemDTO {
    private String productId;
    private Integer quantity;
    private String cartItemId;
}
