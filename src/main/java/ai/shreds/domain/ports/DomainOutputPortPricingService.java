package ai.shreds.domain.ports;

import ai.shreds.application.dtos.ApplicationPricingRequestDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;

/**
 * Domain output port for pricing service integration.
 * This port is implemented by infrastructure layer.
 */
public interface DomainOutputPortPricingService {
    
    /**
     * Calculates pricing for the given request.
     *
     * @param request the pricing calculation request
     * @return the pricing calculation response
     */
    ApplicationPricingResponseDTO calculatePricing(ApplicationPricingRequestDTO request);
}