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

@Component("infrastructurePayPalProcessorAdapter")
public class InfrastructurePayPalProcessorAdapter implements DomainOutputPortPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(InfrastructurePayPalProcessorAdapter.class);
    
    private final InfrastructurePayPalClient payPalClient;
    private final CircuitBreaker circuitBreaker;
    
    @Value("${paypal.webhook-secret:}")
    private String webhookSecret;

    public InfrastructurePayPalProcessorAdapter(
            InfrastructurePayPalClient payPalClient,
            @Qualifier("paypalCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.payPalClient = payPalClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public DomainProcessorChargeResult charge(DomainPaymentIntentEntity intent, DomainThreeDSecureEntity threeDS) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.info("Processing PayPal charge for payment intent: {}", intent.getId().getValue());
                
                // Create PayPal order
                InfrastructurePayPalClient.PayPalOrderRequest orderRequest = 
                    new InfrastructurePayPalClient.PayPalOrderRequest(
                        "CAPTURE",
                        intent.getAmount().getAmount().toString(),
                        intent.getAmount().getCurrency().toUpperCase(),
                        intent.getOrderId().getValue().toString()
                    );
                
                InfrastructurePayPalClient.PayPalOrderResponse orderResponse = 
                    payPalClient.createOrder(orderRequest);
                
                // For PayPal, we typically need user approval, so return processing status
                return mapPayPalResponse(orderResponse);
                
            } catch (Exception e) {
                log.error("Error processing PayPal charge for intent {}: {}", 
                    intent.getId().getValue(), e.getMessage(), e);
                throw handlePayPalException(e);
            }
        });
    }

    @Override
    public DomainUpdateStatusCommand parseWebhook(Map<String, Object> payload) {
        try {
            log.debug("Parsing PayPal webhook payload");
            
            String eventType = (String) payload.get("event_type");
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) payload.get("resource");
            
            String orderId = (String) resource.get("id");
            String status = (String) resource.get("status");
            
            // Map PayPal status to our domain status
            DomainPaymentStatusEnum domainStatus = mapPayPalStatus(status);
            
            // Create a temporary payment ID that will be resolved later by webhook correlation
            // The actual payment ID will be found through the webhook correlation process
            DomainPaymentIdValue tempPaymentId = new DomainPaymentIdValue(UUID.randomUUID());
            
            return new DomainUpdateStatusCommand(
                tempPaymentId, 
                domainStatus,
                payload
            );
            
        } catch (Exception e) {
            log.error("Error parsing PayPal webhook: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "PAYPAL", 
                "WEBHOOK_PARSE_ERROR",
                "Failed to parse PayPal webhook: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    @Override
    public boolean canProcess(DomainPaymentIntentEntity intent) {
        return intent.getProcessorType() == DomainPaymentProcessorTypeEnum.PAYPAL;
    }
    
    @Override
    public String getProcessorType() {
        return "PAYPAL";
    }
    
    @Override
    public boolean validateWebhook(Map<String, Object> payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                log.warn("PayPal webhook secret not configured, skipping signature validation");
                return true; // In development/testing, we might skip validation
            }
            
            if (signature == null || signature.trim().isEmpty()) {
                log.error("PayPal webhook signature is missing");
                return false;
            }
            
            // PayPal uses different webhook validation than Stripe
            // For this implementation, we'll do a basic HMAC SHA256 validation
            // In production, you should use PayPal's webhook signature verification
            
            String payloadString = payload.toString();
            String expectedSignature = computeHmacSha256(webhookSecret, payloadString);
            
            boolean isValid = expectedSignature.equals(signature);
            if (!isValid) {
                log.error("PayPal webhook signature validation failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating PayPal webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private DomainProcessorChargeResult mapPayPalResponse(InfrastructurePayPalClient.PayPalOrderResponse response) {
        DomainPaymentStatusEnum status;
        boolean requiresAction = false;
        Map<String, Object> nextAction = new HashMap<>();
        
        switch (response.getStatus()) {
            case "CREATED":
            case "SAVED":
                status = DomainPaymentStatusEnum.REQUIRES_CONFIRMATION;
                requiresAction = true;
                
                // Find approval URL in links
                for (InfrastructurePayPalClient.PayPalLink link : response.getLinks()) {
                    if ("approve".equals(link.getRel())) {
                        nextAction.put("type", "redirect_to_url");
                        nextAction.put("url", link.getHref());
                        break;
                    }
                }
                break;
            case "APPROVED":
                status = DomainPaymentStatusEnum.PROCESSING;
                break;
            case "COMPLETED":
                status = DomainPaymentStatusEnum.SUCCEEDED;
                break;
            case "CANCELLED":
            case "VOIDED":
            default:
                status = DomainPaymentStatusEnum.FAILED;
                break;
        }
        
        DomainProcessorResponseValue processorResponse = new DomainProcessorResponseValue(
            "PAYPAL",
            response.getStatus(),
            "PayPal order " + response.getStatus().toLowerCase(),
            response.toString()
        );
        
        return new DomainProcessorChargeResult(
            status,
            processorResponse,
            requiresAction,
            nextAction
        );
    }
    
    private DomainPaymentStatusEnum mapPayPalStatus(String paypalStatus) {
        switch (paypalStatus) {
            case "COMPLETED":
                return DomainPaymentStatusEnum.SUCCEEDED;
            case "CREATED":
            case "SAVED":
                return DomainPaymentStatusEnum.REQUIRES_CONFIRMATION;
            case "APPROVED":
            case "PENDING":
                return DomainPaymentStatusEnum.PROCESSING;
            case "CANCELLED":
            case "VOIDED":
            case "DECLINED":
            default:
                return DomainPaymentStatusEnum.FAILED;
        }
    }
    
    private InfrastructureExternalServiceException handlePayPalException(Exception e) {
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
            "PAYPAL",
            errorCode,
            e.getMessage(),
            e,
            isRetryable,
            null
        );
    }
    
    private String computeHmacSha256(String secret, String data) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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