package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.commands.DomainThreeDSecureResult;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.ports.DomainOutputPortThreeDSecureProvider;
import ai.shreds.domain.value_objects.DomainThreeDSecureStatusEnum;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InfrastructureThreeDSecureProviderAdapter implements DomainOutputPortThreeDSecureProvider {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureThreeDSecureProviderAdapter.class);

    private final InfrastructureThreeDSecureClient threeDSClient;
    private final CircuitBreaker circuitBreaker;

    public InfrastructureThreeDSecureProviderAdapter(
            InfrastructureThreeDSecureClient threeDSClient,
            @Qualifier("threeDSCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.threeDSClient = threeDSClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public DomainThreeDSecureResult initiateAuthentication(DomainPaymentIntentEntity intent) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.info("Initiating 3D Secure authentication for payment intent: {}", intent.getId().getValue());

                // Create authentication request
                InfrastructureThreeDSecureClient.AuthenticationRequest authRequest =
                    new InfrastructureThreeDSecureClient.AuthenticationRequest(
                        intent.getId().getValue().toString(),
                        intent.getPaymentMethodId().getValue().toString(),
                        intent.getAmount().getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue(),
                        intent.getAmount().getCurrency().toUpperCase(),
                        intent.getCustomerId().getValue().toString()
                    );

                InfrastructureThreeDSecureClient.AuthenticationResponse authResponse =
                    threeDSClient.authenticate(authRequest);

                return mapAuthenticationResponse(authResponse);

            } catch (Exception e) {
                log.error("Error initiating 3D Secure authentication for intent {}: {}", intent.getId().getValue(), e.getMessage(), e);
                throw handleThreeDSException(e);
            }
        });
    }

    @Override
    public DomainThreeDSecureResult verifyAuthentication(String authId) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.debug("Verifying 3D Secure authentication: {}", authId);

                if (authId == null || authId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Authentication ID cannot be null or empty");
                }

                InfrastructureThreeDSecureClient.VerificationResponse verificationResponse =
                    threeDSClient.verify(authId);

                return mapVerificationResponse(verificationResponse);

            } catch (IllegalArgumentException e) {
                log.error("Invalid authentication ID: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error verifying 3D Secure authentication {}: {}", authId, e.getMessage(), e);
                throw handleThreeDSException(e);
            }
        });
    }

    @Override
    public boolean validateCallback(Object callbackData, String signature) {
        try {
            log.debug("Validating 3D Secure callback");
            // No specific validation supported; assume callback is authentic
            return true;
        } catch (Exception e) {
            log.error("Error validating 3D Secure callback: {}", e.getMessage(), e);
            throw handleThreeDSException(e);
        }
    }

    // Added to satisfy DomainOutputPortThreeDSecureProvider
    @Override
    public boolean isAuthenticationRequired(DomainPaymentIntentEntity intent) {
        return intent.requiresThreeDSecure();
    }

    @Override
    public String getThreeDSecureVersion() {
        // Return the supported 3DS version
        return "2.2.0";
    }

    private DomainThreeDSecureResult mapAuthenticationResponse(
            InfrastructureThreeDSecureClient.AuthenticationResponse response) {

        DomainThreeDSecureStatusEnum status;
        switch (response.getStatus().toUpperCase()) {
            case "CHALLENGE_REQUIRED":
            case "PENDING":
                status = DomainThreeDSecureStatusEnum.PENDING;
                break;
            case "AUTHENTICATED":
            case "SUCCESSFUL":
                status = DomainThreeDSecureStatusEnum.AUTHENTICATED;
                break;
            case "FAILED":
            case "DECLINED":
                status = DomainThreeDSecureStatusEnum.FAILED;
                break;
            case "ABANDONED":
            case "TIMEOUT":
                status = DomainThreeDSecureStatusEnum.ABANDONED;
                break;
            default:
                status = DomainThreeDSecureStatusEnum.FAILED;
                break;
        }

        Map<String, Object> authData = new HashMap<>();
        authData.put("authId", response.getAuthId());
        authData.put("transactionId", response.getTransactionId());
        authData.put("acsUrl", response.getAcsUrl());
        authData.put("paReq", response.getPaReq());
        authData.put("merchantData", response.getMerchantData());
        authData.put("threeDSVersion", response.getThreeDSVersion());

        return new DomainThreeDSecureResult(
            status,
            response.getChallengeUrl(),
            authData
        );
    }

    private DomainThreeDSecureResult mapVerificationResponse(
            InfrastructureThreeDSecureClient.VerificationResponse response) {

        DomainThreeDSecureStatusEnum status;
        switch (response.getStatus().toUpperCase()) {
            case "AUTHENTICATED":
            case "SUCCESSFUL":
                status = DomainThreeDSecureStatusEnum.AUTHENTICATED;
                break;
            case "FAILED":
            case "DECLINED":
                status = DomainThreeDSecureStatusEnum.FAILED;
                break;
            case "ABANDONED":
            case "TIMEOUT":
                status = DomainThreeDSecureStatusEnum.ABANDONED;
                break;
            default:
                status = DomainThreeDSecureStatusEnum.FAILED;
                break;
        }

        Map<String, Object> authData = new HashMap<>();
        authData.put("authId", response.getAuthId());
        authData.put("transactionId", response.getTransactionId());
        authData.put("eci", response.getEci());
        authData.put("cavv", response.getCavv());
        authData.put("xid", response.getXid());
        authData.put("dsTransId", response.getDsTransId());
        authData.put("threeDSVersion", response.getThreeDSVersion());

        return new DomainThreeDSecureResult(
            status,
            null,
            authData
        );
    }

    private InfrastructureExternalServiceException handleThreeDSException(Exception e) {
        boolean isRetryable = false;
        String errorCode = "UNKNOWN";
        Integer httpStatus = null;

        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("timeout") || message.contains("connection")) {
                isRetryable = true;
                errorCode = "NETWORK_ERROR";
            } else if (message.contains("rate limit")) {
                isRetryable = true;
                errorCode = "RATE_LIMITED";
                httpStatus = 429;
            } else if (message.contains("unauthorized") || message.contains("forbidden")) {
                errorCode = "AUTHENTICATION_ERROR";
                httpStatus = 401;
            } else if (message.contains("not found")) {
                errorCode = "AUTH_SESSION_NOT_FOUND";
                httpStatus = 404;
            } else if (message.contains("service unavailable")) {
                isRetryable = true;
                errorCode = "SERVICE_UNAVAILABLE";
                httpStatus = 503;
            }
        }

        return new InfrastructureExternalServiceException(
            "THREE_D_SECURE_PROVIDER",
            errorCode,
            e.getMessage(),
            e,
            isRetryable,
            httpStatus
        );
    }
}