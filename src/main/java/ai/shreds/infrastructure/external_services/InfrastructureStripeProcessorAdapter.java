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
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
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

@Component("infrastructureStripeProcessorAdapter")
public class InfrastructureStripeProcessorAdapter implements DomainOutputPortPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureStripeProcessorAdapter.class);
    
    private final InfrastructureStripeClient stripeClient;
    private final CircuitBreaker circuitBreaker;
    
    @Value("${stripe.api-key}")
    private String apiKey;
    
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public InfrastructureStripeProcessorAdapter(
            InfrastructureStripeClient stripeClient,
            @Qualifier("stripeCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.stripeClient = stripeClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public DomainProcessorChargeResult charge(DomainPaymentIntentEntity intent, DomainThreeDSecureEntity threeDS) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.info("Processing Stripe charge for payment intent: {}", intent.getId().getValue());
                
                // Set the API key for this request
                Stripe.apiKey = apiKey;
                
                // Create Stripe PaymentIntent
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(intent.getAmount().getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue())
                    .setCurrency(intent.getAmount().getCurrency().toLowerCase())
                    .setPaymentMethod(intent.getPaymentMethodId().getValue().toString())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .setConfirm(true)
                    .putMetadata("order_id", intent.getOrderId().getValue().toString())
                    .putMetadata("customer_id", intent.getCustomerId().getValue().toString())
                    .build();
                
                PaymentIntent stripeIntent = PaymentIntent.create(params);
                
                return mapStripeResponse(stripeIntent);
                
            } catch (StripeException e) {
                log.error("Stripe API error for payment intent {}: {}", intent.getId().getValue(), e.getMessage(), e);
                throw handleStripeException(e);
            } catch (Exception e) {
                log.error("Unexpected error processing Stripe charge: {}", e.getMessage(), e);
                throw InfrastructureExternalServiceException.networkError("STRIPE", e);
            }
        });
    }

    @Override
    public DomainUpdateStatusCommand parseWebhook(Map<String, Object> payload) {
        try {
            log.debug("Parsing Stripe webhook payload");
            
            String eventType = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> object = (Map<String, Object>) data.get("object");
            
            String paymentIntentId = (String) object.get("id");
            String status = (String) object.get("status");
            
            // Map Stripe status to our domain status
            DomainPaymentStatusEnum domainStatus = mapStripeStatus(status);
            
            // Extract payment ID from metadata if available
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");
            String orderId = metadata != null ? (String) metadata.get("order_id") : null;
            
            // Create a temporary payment ID that will be resolved later by webhook correlation
            // The actual payment ID will be found through the webhook correlation process
            DomainPaymentIdValue tempPaymentId = new DomainPaymentIdValue(UUID.randomUUID());
            
            return new DomainUpdateStatusCommand(
                tempPaymentId,
                domainStatus,
                payload
            );
            
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "STRIPE", 
                "WEBHOOK_PARSE_ERROR",
                "Failed to parse Stripe webhook: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    @Override
    public boolean canProcess(DomainPaymentIntentEntity intent) {
        return intent.getProcessorType() == DomainPaymentProcessorTypeEnum.STRIPE;
    }
    
    @Override
    public String getProcessorType() {
        return "STRIPE";
    }
    
    @Override
    public boolean validateWebhook(Map<String, Object> payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                log.warn("Stripe webhook secret not configured, skipping signature validation");
                return true; // In development/testing, we might skip validation
            }
            
            if (signature == null || !signature.startsWith("t=")) {
                log.error("Invalid Stripe webhook signature format");
                return false;
            }
            
            // Extract timestamp and signature from the header
            String[] elements = signature.split(",");
            long timestamp = 0;
            String v1Signature = null;
            
            for (String element : elements) {
                String[] keyValue = element.split("=", 2);
                if ("t".equals(keyValue[0])) {
                    timestamp = Long.parseLong(keyValue[1]);
                } else if ("v1".equals(keyValue[0])) {
                    v1Signature = keyValue[1];
                }
            }
            
            if (v1Signature == null) {
                log.error("No v1 signature found in Stripe webhook");
                return false;
            }
            
            // Check timestamp (prevent replay attacks)
            long currentTime = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTime - timestamp) > 300) { // 5 minutes tolerance
                log.error("Stripe webhook timestamp too old: {}", timestamp);
                return false;
            }
            
            // Compute expected signature
            String payloadString = payload.toString();
            String signedPayload = timestamp + "." + payloadString;
            String expectedSignature = computeHmacSha256(webhookSecret, signedPayload);
            
            boolean isValid = expectedSignature.equals(v1Signature);
            if (!isValid) {
                log.error("Stripe webhook signature validation failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating Stripe webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private DomainProcessorChargeResult mapStripeResponse(PaymentIntent stripeIntent) {
        DomainPaymentStatusEnum status;
        boolean requiresAction = false;
        Map<String, Object> nextAction = new HashMap<>();
        
        switch (stripeIntent.getStatus()) {
            case "requires_action":
            case "requires_source_action":
                status = DomainPaymentStatusEnum.PROCESSING;
                requiresAction = true;
                if (stripeIntent.getNextAction() != null && stripeIntent.getNextAction().getRedirectToUrl() != null) {
                    nextAction.put("type", "redirect_to_url");
                    nextAction.put("url", stripeIntent.getNextAction().getRedirectToUrl().getUrl());
                }
                break;
            case "succeeded":
                status = DomainPaymentStatusEnum.SUCCEEDED;
                break;
            case "requires_payment_method":
                status = DomainPaymentStatusEnum.REQUIRES_PAYMENT_METHOD;
                break;
            case "requires_confirmation":
                status = DomainPaymentStatusEnum.REQUIRES_CONFIRMATION;
                break;
            case "processing":
                status = DomainPaymentStatusEnum.PROCESSING;
                break;
            case "canceled":
            case "requires_capture":
            default:
                status = DomainPaymentStatusEnum.FAILED;
                break;
        }
        
        DomainProcessorResponseValue processorResponse = new DomainProcessorResponseValue(
            "STRIPE",
            stripeIntent.getStatus(),
            stripeIntent.getLastPaymentError() != null ? stripeIntent.getLastPaymentError().getMessage() : "Success",
            stripeIntent.toJson()
        );
        
        return new DomainProcessorChargeResult(
            status,
            processorResponse,
            requiresAction,
            nextAction
        );
    }
    
    private DomainPaymentStatusEnum mapStripeStatus(String stripeStatus) {
        switch (stripeStatus) {
            case "succeeded":
                return DomainPaymentStatusEnum.SUCCEEDED;
            case "requires_payment_method":
                return DomainPaymentStatusEnum.REQUIRES_PAYMENT_METHOD;
            case "requires_confirmation":
                return DomainPaymentStatusEnum.REQUIRES_CONFIRMATION;
            case "requires_action":
            case "requires_source_action":
            case "processing":
                return DomainPaymentStatusEnum.PROCESSING;
            case "canceled":
            case "requires_capture":
            default:
                return DomainPaymentStatusEnum.FAILED;
        }
    }
    
    private InfrastructureExternalServiceException handleStripeException(StripeException e) {
        boolean isRetryable = false;
        String errorCode = e.getCode();
        
        // Determine if the error is retryable
        if ("rate_limit".equals(errorCode)) {
            isRetryable = true;
        } else if ("api_connection_error".equals(errorCode)) {
            isRetryable = true;
        }
        
        return new InfrastructureExternalServiceException(
            "STRIPE",
            errorCode != null ? errorCode : "UNKNOWN",
            e.getMessage(),
            e,
            isRetryable,
            e.getStatusCode()
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
