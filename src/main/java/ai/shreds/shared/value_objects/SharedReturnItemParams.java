package ai.shreds.shared.value_objects;

import java.util.Objects;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request parameters for an item in a return request.
 * Updated with Jakarta validation annotations.
 */
public record SharedReturnItemParams(
    @NotBlank String orderItemId,
    @NotNull @Min(1) @Max(999) Integer quantity,
    @NotBlank String reason,
    @NotBlank String condition
) {
    public SharedReturnItemParams {
        Objects.requireNonNull(orderItemId, "orderItemId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (quantity < 1 || quantity > 999) {
            throw new IllegalArgumentException("quantity must be between 1 and 999");
        }
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
    }
}
