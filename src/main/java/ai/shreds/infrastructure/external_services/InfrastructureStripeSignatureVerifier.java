package ai.shreds.infrastructure.external_services;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class InfrastructureStripeSignatureVerifier {

    @Value("${webhook.stripe.signing-secret}")
    private String signingSecret;

    public boolean verify(String payload, String signature) {
        try {
            String[] signatureParts = signature.split(",");
            String timestamp = signatureParts[0].split("=")[1];
            String signedPayload = timestamp + "." + payload;
            
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            
            String computedSignature = Hex.encodeHexString(sha256Hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
            String expectedSignature = signatureParts[1].split("=")[1];
            
            return computedSignature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
