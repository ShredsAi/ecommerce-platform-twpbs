package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortSignatureVerifier;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InfrastructureSignatureVerifierImpl implements DomainOutputPortSignatureVerifier {

    private final InfrastructureStripeSignatureVerifier stripeVerifier;
    private final InfrastructurePayPalSignatureVerifier paypalVerifier;
    private final InfrastructureSquareSignatureVerifier squareVerifier;
    private final ObjectMapper objectMapper;

    public InfrastructureSignatureVerifierImpl(
            InfrastructureStripeSignatureVerifier stripeVerifier,
            InfrastructurePayPalSignatureVerifier paypalVerifier,
            InfrastructureSquareSignatureVerifier squareVerifier,
            ObjectMapper objectMapper) {
        this.stripeVerifier = stripeVerifier;
        this.paypalVerifier = paypalVerifier;
        this.squareVerifier = squareVerifier;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean verifySignature(String rawPayload, String signature, SharedEnumPaymentProcessorType processorType) {
        try {
            switch (processorType) {
                case STRIPE:
                    return stripeVerifier.verify(rawPayload, signature);
                case PAYPAL:
                    // For PayPal, signature contains JSON with headers
                    Map<String, String> headers = objectMapper.readValue(
                            signature,
                            new TypeReference<Map<String, String>>() {}
                    );
                    return paypalVerifier.verify(rawPayload, headers);
                case SQUARE:
                    return squareVerifier.verify(rawPayload, signature);
                default:
                    throw new IllegalArgumentException("Unsupported processor: " + processorType);
            }
        } catch (Exception e) {
            return false;
        }
    }
}