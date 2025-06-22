package ai.shreds.shared.value_objects;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Value object containing all relevant payment processor webhook signature headers.
 * Each processor requires different headers for webhook signature verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedValueWebhookHeaders {
    // Stripe uses a single signature header
    private String stripeSignature;
    
    // PayPal uses multiple headers for certificate-based verification
    private String paypalTransmissionId;
    private String paypalAuthAlgo;
    private String paypalCertUrl;
    private String paypalTransmissionSig;
    
    // Square uses a single signature header
    private String squareSignature;

    /**
     * Builds a map of HTTP headers specific to the given payment processor type.
     * Returns only non-null header values relevant to the specified processor.
     *
     * @param processorType The payment processor type to get headers for
     * @return Map of headers with their corresponding values, empty if processor is null
     */
    public Map<String, String> getProcessorHeaders(SharedEnumPaymentProcessorType processorType) {
        if (processorType == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        
        switch (processorType) {
            case STRIPE:
                if (stripeSignature != null) {
                    headers.put("Stripe-Signature", stripeSignature);
                }
                break;
                
            case PAYPAL:
                if (paypalTransmissionId != null) {
                    headers.put("PayPal-Transmission-Id", paypalTransmissionId);
                }
                if (paypalAuthAlgo != null) {
                    headers.put("PayPal-Auth-Algo", paypalAuthAlgo);
                }
                if (paypalCertUrl != null) {
                    headers.put("PayPal-Cert-Url", paypalCertUrl);
                }
                if (paypalTransmissionSig != null) {
                    headers.put("PayPal-Transmission-Sig", paypalTransmissionSig);
                }
                break;
                
            case SQUARE:
                if (squareSignature != null) {
                    headers.put("X-Square-Signature", squareSignature);
                }
                break;
        }
        
        return headers;
    }
    
    /**
     * Builder utility to create headers for Stripe webhooks.
     * 
     * @param signature The Stripe-Signature header value
     * @return A new SharedValueWebhookHeaders configured for Stripe
     */
    public static SharedValueWebhookHeaders forStripe(String signature) {
        return SharedValueWebhookHeaders.builder()
                .stripeSignature(signature)
                .build();
    }
    
    /**
     * Builder utility to create headers for PayPal webhooks.
     * 
     * @param transmissionId The PayPal-Transmission-Id header value
     * @param authAlgo The PayPal-Auth-Algo header value
     * @param certUrl The PayPal-Cert-Url header value
     * @param transmissionSig The PayPal-Transmission-Sig header value
     * @return A new SharedValueWebhookHeaders configured for PayPal
     */
    public static SharedValueWebhookHeaders forPayPal(
            String transmissionId,
            String authAlgo,
            String certUrl,
            String transmissionSig) {
        return SharedValueWebhookHeaders.builder()
                .paypalTransmissionId(transmissionId)
                .paypalAuthAlgo(authAlgo)
                .paypalCertUrl(certUrl)
                .paypalTransmissionSig(transmissionSig)
                .build();
    }
    
    /**
     * Builder utility to create headers for Square webhooks.
     * 
     * @param signature The X-Square-Signature header value
     * @return A new SharedValueWebhookHeaders configured for Square
     */
    public static SharedValueWebhookHeaders forSquare(String signature) {
        return SharedValueWebhookHeaders.builder()
                .squareSignature(signature)
                .build();
    }
}
