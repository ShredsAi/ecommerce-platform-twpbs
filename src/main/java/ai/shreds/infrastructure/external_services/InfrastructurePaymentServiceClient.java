package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.dtos.PaymentResult;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class InfrastructurePaymentServiceClient implements DomainOutputPortPaymentService {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    @Autowired
    public InfrastructurePaymentServiceClient(RestTemplate restTemplate,
                                            @Value("${payment.service.url:http://localhost:8082}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleAuthorizeError")
    @Retry(name = "payment-service")
    public PaymentResult authorize(DomainOrderEntity order) {
        try {
            String url = paymentServiceUrl + "/api/payments/authorize";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DomainOrderEntity> entity = new HttpEntity<>(order, headers);
            
            ResponseEntity<PaymentResult> response = restTemplate.postForEntity(url, entity, PaymentResult.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "AUTHORIZATION_FAILED", e);
        }
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleCaptureError")
    @Retry(name = "payment-service")
    public PaymentResult capture(String transactionId, SharedMoneyValue amount) {
        try {
            String url = paymentServiceUrl + "/api/payments/capture";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create capture request object
            CaptureRequest captureRequest = new CaptureRequest(transactionId, amount);
            HttpEntity<CaptureRequest> entity = new HttpEntity<>(captureRequest, headers);
            
            ResponseEntity<PaymentResult> response = restTemplate.postForEntity(url, entity, PaymentResult.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "CAPTURE_FAILED", e);
        }
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleRefundError")
    @Retry(name = "payment-service")
    public PaymentResult refund(String transactionId, SharedMoneyValue amount) {
        try {
            String url = paymentServiceUrl + "/api/payments/refund";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create refund request object
            RefundRequest refundRequest = new RefundRequest(transactionId, amount);
            HttpEntity<RefundRequest> entity = new HttpEntity<>(refundRequest, headers);
            
            ResponseEntity<PaymentResult> response = restTemplate.postForEntity(url, entity, PaymentResult.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "REFUND_FAILED", e);
        }
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleCancelError")
    @Retry(name = "payment-service")
    public PaymentResult cancel(String transactionId) {
        try {
            String url = paymentServiceUrl + "/api/payments/" + transactionId + "/cancel";
            ResponseEntity<PaymentResult> response = restTemplate.postForEntity(url, null, PaymentResult.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "CANCELLATION_FAILED", e);
        }
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleCheckStatusError")
    public PaymentResult checkStatus(String transactionId) {
        try {
            String url = paymentServiceUrl + "/api/payments/" + transactionId + "/status";
            ResponseEntity<PaymentResult> response = restTemplate.getForEntity(url, PaymentResult.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "STATUS_CHECK_FAILED", e);
        }
    }

    // Additional refund methods using SharedRefundRequestDTO for specific refund operations
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleRefundRequestError")
    @Retry(name = "payment-service")
    public SharedRefundRequestDTO initiateRefund(SharedRefundRequestDTO request) {
        try {
            String url = paymentServiceUrl + "/api/refunds";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SharedRefundRequestDTO> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<SharedRefundRequestDTO> response = restTemplate.postForEntity(url, entity, SharedRefundRequestDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "REFUND_INITIATION_FAILED", e);
        }
    }

    @CircuitBreaker(name = "payment-service", fallbackMethod = "handleRefundStatusCheckError")
    public SharedRefundRequestDTO checkRefundStatus(String refundId) {
        try {
            String url = paymentServiceUrl + "/api/refunds/" + refundId;
            ResponseEntity<SharedRefundRequestDTO> response = restTemplate.getForEntity(url, SharedRefundRequestDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("PaymentService", "REFUND_STATUS_CHECK_FAILED", e);
        }
    }

    // Fallback methods
    private PaymentResult handleAuthorizeError(DomainOrderEntity order, Exception ex) {
        System.err.println("Payment service fallback triggered for authorize: " + ex.getMessage());
        return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private PaymentResult handleCaptureError(String transactionId, SharedMoneyValue amount, Exception ex) {
        System.err.println("Payment service fallback triggered for capture: " + ex.getMessage());
        return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private PaymentResult handleRefundError(String transactionId, SharedMoneyValue amount, Exception ex) {
        System.err.println("Payment service fallback triggered for refund: " + ex.getMessage());
        return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private PaymentResult handleCancelError(String transactionId, Exception ex) {
        System.err.println("Payment service fallback triggered for cancel: " + ex.getMessage());
        return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private PaymentResult handleCheckStatusError(String transactionId, Exception ex) {
        System.err.println("Payment service fallback triggered for checkStatus: " + ex.getMessage());
        return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private SharedRefundRequestDTO handleRefundRequestError(SharedRefundRequestDTO request, Exception ex) {
        System.err.println("Payment service fallback triggered for initiateRefund: " + ex.getMessage());
        return SharedRefundRequestDTO.builder()
                .refundId(null)
                .orderId(request.getOrderId())
                .cancellationId(request.getCancellationId())
                .returnId(request.getReturnId())
                .amount(request.getAmount())
                .reason(request.getReason())
                .status("FAILED")
                .requestedAt(request.getRequestedAt())
                .processedAt(null)
                .errorMessage("Payment service unavailable")
                .build();
    }

    private SharedRefundRequestDTO handleRefundStatusCheckError(String refundId, Exception ex) {
        System.err.println("Payment service fallback triggered for checkRefundStatus: " + ex.getMessage());
        return SharedRefundRequestDTO.builder()
                .refundId(refundId)
                .status("UNKNOWN")
                .errorMessage("Payment service unavailable")
                .build();
    }

    // Helper classes for request payloads
    private static class CaptureRequest {
        public String transactionId;
        public SharedMoneyValue amount;

        public CaptureRequest(String transactionId, SharedMoneyValue amount) {
            this.transactionId = transactionId;
            this.amount = amount;
        }
    }

    private static class RefundRequest {
        public String transactionId;
        public SharedMoneyValue amount;

        public RefundRequest(String transactionId, SharedMoneyValue amount) {
            this.transactionId = transactionId;
            this.amount = amount;
        }
    }
}
