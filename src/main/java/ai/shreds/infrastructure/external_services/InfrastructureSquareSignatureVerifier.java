package ai.shreds.infrastructure.external_services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InfrastructureSquareSignatureVerifier {

    @Value("${webhook.square.application-secret}")
    private String applicationSecret;

    public boolean verify(String payload, String signature) {
        try {
            if (signature == null || signature.isEmpty()) {
                return false;
            }

            // Combine payload with secret
            String signatureBase = payload + applicationSecret;
            
            // Compute SHA-1 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(signatureBase.getBytes(StandardCharsets.UTF_8));
            
            // Convert to lowercase hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            
            // Compare with provided signature
            return hexString.toString().equals(signature.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }
}
