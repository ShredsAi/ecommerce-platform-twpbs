package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Repository
public class InfrastructureOrderRepositoryImpl implements DomainOutputPortOrderRepository {

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    
    @Autowired
    public InfrastructureOrderRepositoryImpl(RestTemplate restTemplate, 
                                           @Value("${order.service.url:http://localhost:8081}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "fallbackFindOrderSnapshot")
    public SharedOrderSnapshotDTO findOrderSnapshot(String orderId) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId + "/snapshot";
            ResponseEntity<SharedOrderSnapshotDTO> response = restTemplate.getForEntity(url, SharedOrderSnapshotDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("OrderService", "ORDER_NOT_FOUND", e);
        }
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "fallbackUpdateOrderStatus")
    public void updateOrderStatus(String orderId, String newStatus) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId + "/status";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{\"status\":\"" + newStatus + "\"}", headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("OrderService", "ORDER_UPDATE_FAILED", e);
        }
    }

    private SharedOrderSnapshotDTO fallbackFindOrderSnapshot(String orderId, Throwable throwable) {
        // Log the failure for monitoring
        System.err.println("Fallback triggered for findOrderSnapshot - orderId: " + orderId + ", error: " + throwable.getMessage());
        // Return a minimal order snapshot for fallback
        return new SharedOrderSnapshotDTO(
            orderId,
            "UNKNOWN",
            "UNKNOWN",
            null,
            null,
            null,
            "UNKNOWN",
            "UNKNOWN",
            null
        );
    }

    private void fallbackUpdateOrderStatus(String orderId, String newStatus, Throwable throwable) {
        // Log the failure but don't throw exception to avoid cascade failures
        System.err.println("Fallback triggered for updateOrderStatus - orderId: " + orderId + ", status: " + newStatus + ", error: " + throwable.getMessage());
        // Could potentially queue this for retry later or send to a dead letter queue
    }
}