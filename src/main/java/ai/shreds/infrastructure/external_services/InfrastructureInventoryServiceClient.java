package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class InfrastructureInventoryServiceClient implements DomainOutputPortInventoryService {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    @Autowired
    public InfrastructureInventoryServiceClient(RestTemplate restTemplate,
                                                @Value("${inventory.service.url:http://localhost:8083}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackAllocateItems")
    public boolean allocateItems(UUID orderId, List<DomainOrderItemEntity> items) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/allocate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", orderId);
            request.put("items", items);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(url, entity, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("InventoryService", "ALLOCATE_STOCK_FAILED", e);
        }
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReleaseItems")
    public boolean releaseItems(UUID orderId, List<DomainOrderItemEntity> items) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/release";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", orderId);
            request.put("items", items);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(url, entity, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("InventoryService", "RELEASE_STOCK_FAILED", e);
        }
    }

    private boolean fallbackAllocateItems(UUID orderId, List<DomainOrderItemEntity> items, Throwable ex) {
        System.err.println("Inventory service fallback triggered for allocateItems - orderId: " + orderId + ", error: " + ex.getMessage());
        return false;
    }

    private boolean fallbackReleaseItems(UUID orderId, List<DomainOrderItemEntity> items, Throwable ex) {
        System.err.println("Inventory service fallback triggered for releaseItems - orderId: " + orderId + ", error: " + ex.getMessage());
        return false;
    }

}
