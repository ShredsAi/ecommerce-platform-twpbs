package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationPricingRequestDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;

public interface ApplicationPricingOutputPort {
    ApplicationPricingResponseDTO calculatePricing(ApplicationPricingRequestDTO request);
}
