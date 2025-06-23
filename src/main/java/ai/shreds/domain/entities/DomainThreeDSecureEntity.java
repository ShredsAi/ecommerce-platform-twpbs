package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.domain.value_objects.DomainThreeDSecureStatusEnum;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a 3D Secure authentication session.
 */
public class DomainThreeDSecureEntity {
    private final UUID id;
    private final DomainPaymentIntentIdValue paymentIntentId;
    private final String challengeUrl;
    private DomainThreeDSecureStatusEnum status;
    private Map<String, Object> authenticationResult;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DomainThreeDSecureEntity(
            UUID id,
            DomainPaymentIntentIdValue paymentIntentId,
            String challengeUrl,
            DomainThreeDSecureStatusEnum status,
            Map<String, Object> authenticationResult,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.paymentIntentId = Objects.requireNonNull(paymentIntentId, "paymentIntentId cannot be null");
        this.challengeUrl = Objects.requireNonNull(challengeUrl, "challengeUrl cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.authenticationResult = authenticationResult != null ? new HashMap<>(authenticationResult) : new HashMap<>();
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        
        validateBusinessRules();
    }

    /**
     * Static factory method to create a new 3D Secure authentication session.
     */
    public static DomainThreeDSecureEntity create(
            UUID id,
            DomainPaymentIntentIdValue paymentIntentId,
            String challengeUrl,
            LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new DomainThreeDSecureEntity(
                id,
                paymentIntentId,
                challengeUrl,
                DomainThreeDSecureStatusEnum.PENDING,
                new HashMap<>(),
                expiresAt,
                now,
                now
        );
    }

    /**
     * Checks if the authentication is complete and successful.
     */
    public boolean isAuthenticated() {
        return status == DomainThreeDSecureStatusEnum.AUTHENTICATED;
    }

    /**
     * Checks if the authentication is still pending.
     */
    public boolean isPending() {
        return status == DomainThreeDSecureStatusEnum.PENDING;
    }

    /**
     * Checks if the authentication has failed.
     */
    public boolean isFailed() {
        return status == DomainThreeDSecureStatusEnum.FAILED;
    }

    /**
     * Checks if the authentication was abandoned by the user.
     */
    public boolean isAbandoned() {
        return status == DomainThreeDSecureStatusEnum.ABANDONED;
    }

    /**
     * Checks if the authentication session has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Sets the authentication as successful with the provided result.
     * @param result the authentication result from the 3DS provider
     */
    public void setAuthenticated(Map<String, Object> result) {
        if (isExpired()) {
            throw new IllegalStateException("Cannot authenticate expired 3D Secure session");
        }
        
        if (status != DomainThreeDSecureStatusEnum.PENDING) {
            throw new IllegalStateException("Can only authenticate pending 3D Secure sessions, current status: " + status);
        }
        
        this.status = DomainThreeDSecureStatusEnum.AUTHENTICATED;
        this.authenticationResult = result != null ? new HashMap<>(result) : new HashMap<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the authentication as failed.
     * @param result the failure details from the 3DS provider
     */
    public void setFailed(Map<String, Object> result) {
        if (status != DomainThreeDSecureStatusEnum.PENDING) {
            throw new IllegalStateException("Can only fail pending 3D Secure sessions, current status: " + status);
        }
        
        this.status = DomainThreeDSecureStatusEnum.FAILED;
        this.authenticationResult = result != null ? new HashMap<>(result) : new HashMap<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the authentication as abandoned by the user.
     */
    public void setAbandoned() {
        if (status != DomainThreeDSecureStatusEnum.PENDING) {
            throw new IllegalStateException("Can only abandon pending 3D Secure sessions, current status: " + status);
        }
        
        this.status = DomainThreeDSecureStatusEnum.ABANDONED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateBusinessRules() {
        if (challengeUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Challenge URL cannot be empty");
        }
        
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiry time cannot be before creation time");
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public DomainPaymentIntentIdValue getPaymentIntentId() {
        return paymentIntentId;
    }

    public String getChallengeUrl() {
        return challengeUrl;
    }

    public DomainThreeDSecureStatusEnum getStatus() {
        return status;
    }

    public Map<String, Object> getAuthenticationResult() {
        return new HashMap<>(authenticationResult);
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainThreeDSecureEntity)) return false;
        DomainThreeDSecureEntity that = (DomainThreeDSecureEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainThreeDSecureEntity{" +
                "id=" + id +
                ", paymentIntentId=" + paymentIntentId +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}