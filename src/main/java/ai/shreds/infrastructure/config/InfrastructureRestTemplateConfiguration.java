package ai.shreds.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate used by external service clients.
 * Provides optimized HTTP client settings for payment processing workloads.
 */
@Configuration
public class InfrastructureRestTemplateConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        // Configure connection and read timeouts for payment processing
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Connection timeout: time to establish connection
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        
        // Read timeout: time to wait for response after connection established
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        
        // Buffer request/response for debugging and retry scenarios
        BufferingClientHttpRequestFactory bufferingFactory = 
            new BufferingClientHttpRequestFactory(factory);
        
        RestTemplate restTemplate = new RestTemplate(bufferingFactory);
        
        // Add interceptors for logging, correlation IDs, and error handling
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Add correlation ID header for tracing
            String correlationId = java.util.UUID.randomUUID().toString();
            request.getHeaders().add("X-Correlation-ID", correlationId);
            
            // Add user agent
            request.getHeaders().add("User-Agent", "PaymentProcessingService/1.0.0");
            
            // Log outgoing request
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("HTTP_CLIENT");
            log.debug("Outgoing HTTP {} request to {} with correlation ID: {}", 
                request.getMethod(), request.getURI(), correlationId);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Execute the request
                org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
                
                long duration = System.currentTimeMillis() - startTime;
                log.debug("HTTP response {} received in {} ms for correlation ID: {}", 
                    response.getStatusCode(), duration, correlationId);
                
                return response;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("HTTP request failed after {} ms for correlation ID {}: {}", 
                    duration, correlationId, e.getMessage());
                throw e;
            }
        });
        
        return restTemplate;
    }
}