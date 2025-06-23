package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event representing that 3D Secure authentication is required for a payment intent.
 */
public class DomainThreeDSecureRequiredEvent {
    private final DomainPaymentIntentIdValue intentId;
    private final String challengeUrl;
    private final LocalDateTime timestamp;

    public DomainThreeDSecureRequiredEvent(
            DomainPaymentIntentIdValue intentId,
            String challengeUrl,
            LocalDateTime timestamp) {
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.challengeUrl = Objects.requireNonNull(challengeUrl, "challengeUrl cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        
        validateChallengeUrl();
    }

    private void validateChallengeUrl() {
        if (challengeUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("challengeUrl cannot be empty");
        }
        
        // Basic URL validation
        if (!challengeUrl.startsWith("http://") && !challengeUrl.startsWith("https://")) {
            throw new IllegalArgumentException("challengeUrl must be a valid HTTP/HTTPS URL");
        }
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public String getChallengeUrl() {
        return challengeUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainThreeDSecureRequiredEvent)) return false;
        DomainThreeDSecureRequiredEvent that = (DomainThreeDSecureRequiredEvent) o;
        return intentId.equals(that.intentId) && 
               challengeUrl.equals(that.challengeUrl) && 
               timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentId, challengeUrl, timestamp);
    }

    @Override
    public String toString() {
        return "DomainThreeDSecureRequiredEvent{" +
                "intentId=" + intentId +
                ", challengeUrl='" + challengeUrl + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}