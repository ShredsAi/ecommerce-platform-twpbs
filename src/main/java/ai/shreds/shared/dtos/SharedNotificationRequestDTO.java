package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * DTO representing a notification request to be sent to customers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedNotificationRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Notification type is required")
    private String type;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    @Builder.Default
    private String priority = "NORMAL";

    @NotNull(message = "Timestamp is required")
    @Builder.Default
    private Instant timestamp = Instant.now();

    public void validate() {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }

    public SharedNotificationRequestDTO addDataItem(String key, Object value) {
        if (key != null && !key.trim().isEmpty()) {
            if (data == null) {
                data = new HashMap<>();
            }
            data.put(key, value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getFromData(String key, Class<T> type) {
        if (data == null || !data.containsKey(key)) {
            return null;
        }
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public boolean isHighPriority() { return "HIGH".equalsIgnoreCase(priority); }

    public boolean isLowPriority() { return "LOW".equalsIgnoreCase(priority); }

    public static SharedNotificationRequestDTO createHighPriority(String customerId, String type) {
        return SharedNotificationRequestDTO.builder()
                .customerId(customerId)
                .type(type)
                .priority("HIGH")
                .timestamp(Instant.now())
                .build();
    }

    public static SharedNotificationRequestDTO createLowPriority(String customerId, String type) {
        return SharedNotificationRequestDTO.builder()
                .customerId(customerId)
                .type(type)
                .priority("LOW")
                .timestamp(Instant.now())
                .build();
    }
}