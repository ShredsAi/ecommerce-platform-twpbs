package ai.shreds.shared.value_objects;

import java.util.List;
import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request parameters for a return request with proper Jakarta validation.
 */
public record SharedReturnRequestParams(
    @NotBlank String orderId,
    @NotNull @Size(min = 1) @Valid List<SharedReturnItemParams> items,
    @NotBlank String reason,
    String notes
) {
    public SharedReturnRequestParams {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must contain at least one element");
        }
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
