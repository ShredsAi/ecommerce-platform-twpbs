package ai.shreds.application.dtos;

import java.util.UUID;
import java.time.LocalDateTime;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;

/**
 * DTO representing a payment intent in the application layer
 */
public class ApplicationPaymentIntentDTO {

    private UUID id;
    private String clientSecret;
    private String status;
    private LocalDateTime expiresAt;

    public ApplicationPaymentIntentDTO() {}

    public ApplicationPaymentIntentDTO(UUID id, String clientSecret, String status, LocalDateTime expiresAt) {
        this.id = id;
        this.clientSecret = clientSecret;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Creates an ApplicationPaymentIntentDTO from a domain entity
     * Fixed to avoid circular dependency by mapping directly
     */
    public static ApplicationPaymentIntentDTO fromDomainEntity(DomainPaymentIntentEntity entity) {
        return new ApplicationPaymentIntentDTO(
            entity.getId().getValue(),
            entity.getClientSecret(),
            entity.getStatus().name(),
            entity.getExpiresAt()
        );
    }
}