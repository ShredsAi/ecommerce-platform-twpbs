package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainThreeDSecureStatusEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a 3-D Secure authentication flow.
 * This class encapsulates the outcome of 3DS authentication attempts.
 */
public class DomainThreeDSecureResult {
    private final DomainThreeDSecureStatusEnum status;
    private final String challengeUrl;
    private final Map<String, Object> authenticationData;

    public DomainThreeDSecureResult(
            DomainThreeDSecureStatusEnum status,
            String challengeUrl,
            Map<String, Object> authenticationData) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.challengeUrl = challengeUrl; // Can be null for some statuses
        this.authenticationData = authenticationData != null ? 
            new HashMap<>(authenticationData) : new HashMap<>();
        
        validateResult();
    }

    /**
     * Creates a successful 3DS authentication result.
     */
    public static DomainThreeDSecureResult authenticated(Map<String, Object> authenticationData) {
        return new DomainThreeDSecureResult(
            DomainThreeDSecureStatusEnum.AUTHENTICATED,
            null, // No challenge URL needed for successful auth
            authenticationData
        );
    }

    /**
     * Creates a pending 3DS authentication result with challenge URL.
     */
    public static DomainThreeDSecureResult pending(String challengeUrl) {
        Objects.requireNonNull(challengeUrl, "challengeUrl cannot be null for pending status");
        return new DomainThreeDSecureResult(
            DomainThreeDSecureStatusEnum.PENDING,
            challengeUrl,
            new HashMap<>()
        );
    }

    /**
     * Creates a failed 3DS authentication result.
     */
    public static DomainThreeDSecureResult failed(String failureReason) {
        Map<String, Object> data = new HashMap<>();
        if (failureReason != null) {
            data.put("failure_reason", failureReason);
        }
        return new DomainThreeDSecureResult(
            DomainThreeDSecureStatusEnum.FAILED,
            null,
            data
        );
    }

    /**
     * Creates an abandoned 3DS authentication result.
     */
    public static DomainThreeDSecureResult abandoned() {
        return new DomainThreeDSecureResult(
            DomainThreeDSecureStatusEnum.ABANDONED,
            null,
            new HashMap<>()
        );
    }

    private void validateResult() {
        switch (status) {
            case PENDING:
                if (challengeUrl == null || challengeUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("Challenge URL is required for pending status");
                }
                break;
            case AUTHENTICATED:
                // Authentication data should be present for successful auth
                if (authenticationData.isEmpty()) {
                    throw new IllegalArgumentException("Authentication data should be present for authenticated status");
                }
                break;
            case FAILED:
            case ABANDONED:
                // These states don't require challenge URL
                break;
        }
    }

    public DomainThreeDSecureStatusEnum getStatus() {
        return status;
    }

    public String getChallengeUrl() {
        return challengeUrl;
    }

    public Map<String, Object> getAuthenticationData() {
        return new HashMap<>(authenticationData);
    }

    /**
     * Checks if the 3DS authentication was successful.
     */
    public boolean isAuthenticated() {
        return status == DomainThreeDSecureStatusEnum.AUTHENTICATED;
    }

    /**
     * Checks if the 3DS authentication is still pending.
     */
    public boolean isPending() {
        return status == DomainThreeDSecureStatusEnum.PENDING;
    }

    /**
     * Checks if the 3DS authentication failed.
     */
    public boolean isFailed() {
        return status == DomainThreeDSecureStatusEnum.FAILED;
    }

    /**
     * Checks if the 3DS authentication was abandoned.
     */
    public boolean isAbandoned() {
        return status == DomainThreeDSecureStatusEnum.ABANDONED;
    }

    /**
     * Gets the authentication transaction ID if available.
     */
    public String getAuthenticationTransactionId() {
        Object transactionId = authenticationData.get("transaction_id");
        return transactionId != null ? transactionId.toString() : null;
    }

    /**
     * Gets the failure reason if authentication failed.
     */
    public String getFailureReason() {
        if (!isFailed()) {
            return null;
        }
        Object reason = authenticationData.get("failure_reason");
        return reason != null ? reason.toString() : "Unknown failure";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainThreeDSecureResult)) return false;
        DomainThreeDSecureResult that = (DomainThreeDSecureResult) o;
        return status == that.status && 
               Objects.equals(challengeUrl, that.challengeUrl) && 
               authenticationData.equals(that.authenticationData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, challengeUrl, authenticationData);
    }

    @Override
    public String toString() {
        return "DomainThreeDSecureResult{" +
                "status=" + status +
                ", challengeUrl='" + challengeUrl + '\'' +
                ", authenticationData=" + authenticationData +
                '}';
    }
}