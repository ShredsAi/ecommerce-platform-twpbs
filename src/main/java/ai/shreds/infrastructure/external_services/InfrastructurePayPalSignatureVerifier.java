package ai.shreds.infrastructure.external_services;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

@Component
public class InfrastructurePayPalSignatureVerifier {

    @Value("${webhook.paypal.cert-url:}")
    private String configuredCertUrl;
    
    public boolean verify(String payload, Map<String, String> headers) {
        try {
            String transmissionId = headers.get("PayPal-Transmission-Id");
            String transmissionSig = headers.get("PayPal-Transmission-Sig");
            String transmissionTime = headers.get("PayPal-Transmission-Time");
            String authAlgo = headers.get("PayPal-Auth-Algo");
            String certUrl = headers.get("PayPal-Cert-Url");
            
            // Use configured cert URL if provided, otherwise use the one from headers
            if (configuredCertUrl != null && !configuredCertUrl.isEmpty()) {
                certUrl = configuredCertUrl;
            }
            
            // Validate all required headers are present
            if (transmissionId == null || transmissionSig == null || transmissionTime == null || 
                authAlgo == null || certUrl == null) {
                return false;
            }
            
            // Download certificate from PayPal
            PublicKey publicKey = getPayPalPublicKey(certUrl);
            
            // Construct the data string for verification
            String verificationData = transmissionId + "|" + transmissionTime + "|" + payload;
            
            // Verify signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(verificationData.getBytes("UTF-8"));
            
            byte[] decodedSignature = Base64.decodeBase64(transmissionSig);
            return signature.verify(decodedSignature);
        } catch (Exception e) {
            return false;
        }
    }
    
    private PublicKey getPayPalPublicKey(String certUrl) {
        try {
            URL url = new URL(certUrl);
            InputStream inStream = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(inStream);
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
            bis.close();
            inStream.close();
            
            return cert.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve PayPal certificate", e);
        }
    }
}
