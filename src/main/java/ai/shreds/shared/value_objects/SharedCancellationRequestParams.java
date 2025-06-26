package ai.shreds.shared.value_objects;

import java.util.Objects;
import jakarta.validation.constraints.NotBlank;

/**
 * Request parameters for cancellation with proper Jakarta validation.
 */
public record SharedCancellationRequestParams(
    @NotBlank String orderId,
    @NotBlank String reason,
    String notes
) {
    public SharedCancellationRequestParams {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
