package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.commands.DomainUpdateStatusCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;
import ai.shreds.domain.ports.DomainOutputPortPaymentProcessor;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;
import ai.shreds.domain.value_objects.DomainProcessorResponseValue;
import ai.shreds.domain.value_objects.DomainPaymentProcessorTypeEnum;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component("infrastructureSquareProcessorAdapter")
public class InfrastructureSquareProcessorAdapter implements DomainOutputPortPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureSquareProcessorAdapter.class);
    
    private final InfrastructureSquareClient squareClient;
    private final CircuitBreaker circuitBreaker;
    
    @Value("${square.webhook-signature-key:}")
    private String webhookSignatureKey;

    public InfrastructureSquareProcessorAdapter(
            InfrastructureSquareClient squareClient,
            @Qualifier("squareCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.squareClient = squareClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public DomainProcessorChargeResult charge(DomainPaymentIntentEntity intent, DomainThreeDSecureEntity threeDS) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.info("Processing Square charge for payment intent: {}", intent.getId().getValue());
                
                // Create Square payment
                InfrastructureSquareClient.SquarePaymentRequest paymentRequest = 
                    new InfrastructureSquareClient.SquarePaymentRequest(
                        intent.getPaymentMethodId().getValue().toString(), // source_id
                        intent.getId().getValue().toString(), // idempotency_key
                        intent.getAmount().getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue(),
                        intent.getAmount().getCurrency().toUpperCase()
                    );
                
                InfrastructureSquareClient.SquarePaymentResponse paymentResponse = 
                    squareClient.createPayment(paymentRequest);
                
                return mapSquareResponse(paymentResponse);
                
            } catch (Exception e) {
                log.error("Error processing Square charge for intent {}: {}", 
                    intent.getId().getValue(), e.getMessage(), e);
                throw handleSquareException(e);
            }
        });
    }

    @Override
    public DomainUpdateStatusCommand parseWebhook(Map<String, Object> payload) {
        try {
            log.debug("Parsing Square webhook payload");
            
            String eventType = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> object = (Map<String, Object>) data.get("object");
            @SuppressWarnings("unchecked")
            Map<String, Object> payment = (Map<String, Object>) object.get("payment");
            
            String paymentId = (String) payment.get("id");
            String status = (String) payment.get("status");
            
            // Map Square status to our domain status
            DomainPaymentStatusEnum domainStatus = mapSquareStatus(status);
            
            // Create a temporary payment ID that will be resolved later by webhook correlation
            // The actual payment ID will be found through the webhook correlation process
            DomainPaymentIdValue tempPaymentId = new DomainPaymentIdValue(UUID.randomUUID());
            
            return new DomainUpdateStatusCommand(
                tempPaymentId,
                domainStatus,
                payload
            );
            
        } catch (Exception e) {
            log.error("Error parsing Square webhook: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "SQUARE", 
                "WEBHOOK_PARSE_ERROR",
                "Failed to parse Square webhook: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    @Override
    public boolean canProcess(DomainPaymentIntentEntity intent) {
        return intent.getProcessorType() == DomainPaymentProcessorTypeEnum.SQUARE;
    }
    
    @Override
    public String getProcessorType() {
        return "SQUARE";
    }
    
    @Override
    public boolean validateWebhook(Map<String, Object> payload, String signature) {
        try {
            if (webhookSignatureKey == null || webhookSignatureKey.trim().isEmpty()) {
                log.warn("Square webhook signature key not configured, skipping signature validation");
                return true; // In development/testing, we might skip validation
            }
            
            if (signature == null || signature.trim().isEmpty()) {
                log.error("Square webhook signature is missing");
                return false;
            }
            
            // Square uses HMAC SHA1 for webhook signature validation
            String payloadString = payload.toString();
            String expectedSignature = computeHmacSha1(webhookSignatureKey, payloadString);
            
            boolean isValid = expectedSignature.equals(signature);
            if (!isValid) {
                log.error("Square webhook signature validation failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating Square webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private DomainProcessorChargeResult mapSquareResponse(InfrastructureSquareClient.SquarePaymentResponse response) {
        DomainPaymentStatusEnum status;
        boolean requiresAction = false;
        Map<String, Object> nextAction = new HashMap<>();
        
        switch (response.getStatus()) {
            case "APPROVED":
            case "COMPLETED":
                status = DomainPaymentStatusEnum.SUCCEEDED;
                break;
            case "PENDING":
                status = DomainPaymentStatusEnum.PROCESSING;
                break;
            case "CANCELLED":
            case "FAILED":
            default:
                status = DomainPaymentStatusEnum.FAILED;
                break;
        }
        
        DomainProcessorResponseValue processorResponse = new DomainProcessorResponseValue(
            "SQUARE",
            response.getStatus(),
            "Square payment " + response.getStatus().toLowerCase(),
            response.toString()
        );
        
        return new DomainProcessorChargeResult(
            status,
            processorResponse,
            requiresAction,
            nextAction
        );
    }
    
    private DomainPaymentStatusEnum mapSquareStatus(String squareStatus) {
        switch (squareStatus) {
            case "APPROVED":
            case "COMPLETED":
                return DomainPaymentStatusEnum.SUCCEEDED;
            case "PENDING":
                return DomainPaymentStatusEnum.PROCESSING;
            case "CANCELLED":
            case "FAILED":
            default:
                return DomainPaymentStatusEnum.FAILED;
        }
    }
    
    private InfrastructureExternalServiceException handleSquareException(Exception e) {
        boolean isRetryable = false;
        String errorCode = "UNKNOWN";
        
        // Determine if the error is retryable based on exception type or message
        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("timeout") || message.contains("connection")) {
                isRetryable = true;
                errorCode = "NETWORK_ERROR";
            } else if (message.contains("rate limit")) {
                isRetryable = true;
                errorCode = "RATE_LIMITED";
            }
        }
        
        return new InfrastructureExternalServiceException(
            "SQUARE",
            errorCode,
            e.getMessage(),
            e,
            isRetryable,
            null
        );
    }
    
    private String computeHmacSha1(String secret, String data) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}