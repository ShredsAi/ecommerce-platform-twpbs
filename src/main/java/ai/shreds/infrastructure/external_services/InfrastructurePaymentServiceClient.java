package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client for Payment Service, implements resilient refund operations.
 */
@Service
public class InfrastructurePaymentServiceClient implements ApplicationPaymentServiceOutputPort {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public InfrastructurePaymentServiceClient(RestTemplate restTemplate,
                                               @Value("${payment.service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "payment-service", fallbackMethod = "handlePaymentServiceError")
    @Retry(name = "payment-service")
    public SharedRefundRequestDTO initiateRefund(SharedRefundRequestDTO request) {
        return restTemplate.postForObject(paymentServiceUrl + "/refunds", request, SharedRefundRequestDTO.class);
    }

    @Override
    public SharedRefundRequestDTO checkRefundStatus(String refundId) {
        return restTemplate.getForObject(paymentServiceUrl + "/refunds/" + refundId,
                SharedRefundRequestDTO.class);
    }

    @Override
    public void cancelRefund(String refundId) {
        restTemplate.delete(paymentServiceUrl + "/refunds/" + refundId);
    }

    /**
     * Fallback for payment service errors.
     */
    @SuppressWarnings("unused")
    public SharedRefundRequestDTO handlePaymentServiceError(SharedRefundRequestDTO request, Throwable ex) {
        throw new InfrastructureExternalServiceException("PaymentService", ex.getMessage(), ex);
    }
}